// Configuración global de Chart.js
Chart.defaults.color = '#666';
Chart.defaults.font.family = "'Segoe UI', 'Arial', sans-serif";

// Estado global
let currentFilters = {
    fechaInicio: null,
    fechaFin: null,
    tipoVenta: '',
    metodoPago: ''
};

let charts = {
    metodosPago: null
};

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
    initializeDashboard();
});

function initializeDashboard() {
    setupDateDefaults();
    setupEventListeners();
    loadDashboardData();
}

function setupDateDefaults() {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    
    const fechaInicio = document.getElementById('fecha-inicio');
    const fechaFin = document.getElementById('fecha-fin');
    
    if (fechaInicio && fechaFin) {
        fechaInicio.valueAsDate = firstDay;
        fechaFin.valueAsDate = today;
        
        currentFilters.fechaInicio = fechaInicio.value;
        currentFilters.fechaFin = fechaFin.value;
    }
}

function setupEventListeners() {
    // Filtros
    const filtrarBtn = document.getElementById('filtrar-reporte');
    if (filtrarBtn) {
        filtrarBtn.addEventListener('click', () => {
            updateFilters();
            loadDashboardData();
        });
    }

    // Exportación
    document.querySelectorAll('.export-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const format = btn.getAttribute('data-format');
            exportReport(format);
        });
    });

    // Actualización automática al cambiar filtros
    ['fecha-inicio', 'fecha-fin', 'tipo-venta', 'metodo-pago'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', () => {
                validateDates();
            });
        }
    });
}

function validateDates() {
    const fechaInicio = document.getElementById('fecha-inicio');
    const fechaFin = document.getElementById('fecha-fin');
    const filtrarBtn = document.getElementById('filtrar-reporte');
    
    if (!fechaInicio || !fechaFin || !filtrarBtn) return;
    
    const inicio = new Date(fechaInicio.value);
    const fin = new Date(fechaFin.value);
    const hoy = new Date();
    
    let isValid = true;
    let errorMessage = '';
    
    // Resetear estilos
    fechaInicio.classList.remove('is-invalid');
    fechaFin.classList.remove('is-invalid');
    
    // Validaciones
    if (inicio > fin) {
        isValid = false;
        errorMessage = 'La fecha de inicio no puede ser posterior a la fecha fin';
        fechaInicio.classList.add('is-invalid');
        fechaFin.classList.add('is-invalid');
    } else if (inicio > hoy || fin > hoy) {
        isValid = false;
        errorMessage = 'No se pueden seleccionar fechas futuras';
        if (inicio > hoy) fechaInicio.classList.add('is-invalid');
        if (fin > hoy) fechaFin.classList.add('is-invalid');
    }
    
    filtrarBtn.disabled = !isValid;
    
    if (!isValid && errorMessage) {
        showNotification(errorMessage, 'warning');
    }
    
    return isValid;
}

function updateFilters() {
    const elements = {
        fechaInicio: document.getElementById('fecha-inicio'),
        fechaFin: document.getElementById('fecha-fin'),
        tipoVenta: document.getElementById('tipo-venta'),
        metodoPago: document.getElementById('metodo-pago')
    };

    for (const [key, element] of Object.entries(elements)) {
        if (element) {
            currentFilters[key] = element.value;
        }
    }
}

async function loadDashboardData() {
    if (!validateDates()) return;
    
    showLoading(true);
    
    try {
        const params = new URLSearchParams(currentFilters);
        const response = await fetch(`/reportes/ventas/data?${params.toString()}`);
        
        if (!response.ok) {
            throw new Error(`Error del servidor: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.error) {
            throw new Error(data.error);
        }
        
        updateDashboard(data);
        // Solo mostrar notificación si hay datos
        if (!isDashboardEmpty(data)) {
            showNotification('Dashboard actualizado', 'success');
        }
        
    } catch (error) {
        console.error('Error cargando datos:', error);
        showNotification('Error al cargar los datos: ' + error.message, 'error');
        showEmptyState('Error al cargar los datos');
    } finally {
        showLoading(false);
    }
}

function updateDashboard(data) {
    if (isDashboardEmpty(data)) {
        showEmptyState('No hay datos para los filtros seleccionados');
        return;
    }

    updateStats(data);
    updateCharts(data);
}

function isDashboardEmpty(data) {
    return !data || (
        (!data.ventasPorMetodoPago || Object.keys(data.ventasPorMetodoPago).length === 0) &&
        (!data.totalVentas || data.totalVentas === 0)
    );
}

function updateStats(data) {
    const stats = {
        'total-ventas': data.totalVentas || 0,
        'ingresos-totales': formatCurrency(data.totalIngresos || 0),
        'ingresos-totales-bs': formatCurrencyBs(data.subtotalBolivares || 0),
        'ticket-promedio': formatCurrency(data.ticketPromedio || 0),
        'productos-vendidos': data.productosVendidos || 0
    };
    for (const [id, value] of Object.entries(stats)) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }
    // Mostrar tasas actuales
    if (data.tasas) {
        document.getElementById('tasa-bcv').textContent = formatCurrencyBs(data.tasas.BCV || 0);
        document.getElementById('tasa-promedio').textContent = formatCurrencyBs(data.tasas.PROMEDIO || 0);
        document.getElementById('tasa-paralela').textContent = formatCurrencyBs(data.tasas.PARALELA || 0);
    }
}

function updateCharts(data) {
    updateMetodosPagoChart(data.ventasPorMetodoPago || {});
}

function updateMetodosPagoChart(data) {
    const ctx = document.getElementById('metodosPagoChart');
    if (!ctx) return;

    if (charts.metodosPago) {
        charts.metodosPago.destroy();
    }

    const chartData = prepareMetodosPagoChartData(data);

    charts.metodosPago = new Chart(ctx, {
        type: 'doughnut',
        data: chartData,
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '60%',
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        padding: 20,
                        usePointStyle: true,
                        pointStyle: 'circle'
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    titleColor: '#333',
                    bodyColor: '#666',
                    borderColor: '#ddd',
                    borderWidth: 1,
                    padding: 12,
                    displayColors: true,
                    callbacks: {
                        label: (context) => {
                            const value = context.raw;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return ` ${context.label}: $${value.toFixed(2)} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

function prepareMetodosPagoChartData(data) {
    const colors = [
        'rgba(21, 101, 192, 0.8)',
        'rgba(0, 191, 165, 0.8)',
        'rgba(25, 118, 210, 0.8)',
        'rgba(13, 71, 161, 0.8)',
        'rgba(0, 121, 107, 0.8)'
    ];

    const borderColors = colors.map(color => color.replace('0.8', '1'));

    return {
        labels: Object.keys(data),
        datasets: [{
            data: Object.values(data),
            backgroundColor: colors,
            borderColor: borderColors,
            borderWidth: 2,
            hoverOffset: 15
        }]
    };
}

function showEmptyState(message = 'No hay datos disponibles') {
    // Limpiar estadísticas
    ['total-ventas', 'ingresos-totales', 'ticket-promedio', 'productos-vendidos'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = id.includes('total') ? '0' : '$0.00';
        }
    });

    // Limpiar y mostrar mensaje en gráficos
    ['metodosPagoChart'].forEach(id => {
        const canvas = document.getElementById(id);
        if (!canvas) return;

        if (charts[id]) {
            charts[id].destroy();
            charts[id] = null;
        }

        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = '#666';
        ctx.font = '14px "Segoe UI", Arial, sans-serif';
        ctx.fillText(message, canvas.width / 2, canvas.height / 2);
    });
}

function showLoading(show) {
    const existingOverlay = document.querySelector('.loading-overlay');
    if (show && !existingOverlay) {
        const overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        overlay.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(overlay);
        document.body.style.cursor = 'wait';
    } else if (!show && existingOverlay) {
        existingOverlay.remove();
        document.body.style.cursor = 'default';
    }
}

function exportReport(format) {
    const params = new URLSearchParams(currentFilters);
    window.location.href = `/reportes/ventas/exportar/${format}?${params.toString()}`;
}

function formatCurrency(value) {
    return '$' + Number(value).toFixed(2);
}

function formatCurrencyBs(value) {
    return 'Bs ' + Number(value).toFixed(2);
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => {
            notification.remove();
        }, 300);
    }, 3000);
}