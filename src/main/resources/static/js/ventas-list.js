// Estado global
let currentFilters = {
    fechaInicio: null,
    fechaFin: null,
    tipoVenta: '',
    metodoPago: '',
    page: 0,
    size: 10
};

let totalPages = 0;
let currentSort = {
    field: 'fechaVenta',
    direction: 'desc'
};

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
    initializeVentasList();
});

function initializeVentasList() {
    setupDateDefaults();
    setupEventListeners();
    loadVentasData();
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
    const filtrarBtn = document.getElementById('filtrar-ventas');
    if (filtrarBtn) {
        filtrarBtn.addEventListener('click', () => {
            currentFilters.page = 0; // Reset a la primera página
            updateFilters();
            loadVentasData();
        });
    }

    // Paginación
    document.getElementById('prev-page')?.addEventListener('click', () => {
        if (currentFilters.page > 0) {
            currentFilters.page--;
            loadVentasData();
        }
    });

    document.getElementById('next-page')?.addEventListener('click', () => {
        if (currentFilters.page < totalPages - 1) {
            currentFilters.page++;
            loadVentasData();
        }
    });

    // Ordenamiento
    document.querySelectorAll('.sortable').forEach(th => {
        th.addEventListener('click', () => {
            const field = th.getAttribute('data-field');
            if (field) {
                if (currentSort.field === field) {
                    currentSort.direction = currentSort.direction === 'asc' ? 'desc' : 'asc';
                } else {
                    currentSort.field = field;
                    currentSort.direction = 'asc';
                }
                updateSortIndicators();
                loadVentasData();
            }
        });
    });

    // Tamaño de página
    const pageSizeSelect = document.getElementById('page-size');
    if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', () => {
            currentFilters.size = parseInt(pageSizeSelect.value);
            currentFilters.page = 0; // Reset a la primera página
            loadVentasData();
        });
    }

    // Validación de fechas
    ['fecha-inicio', 'fecha-fin'].forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.addEventListener('change', validateDates);
        }
    });
}

function validateDates() {
    const fechaInicio = document.getElementById('fecha-inicio');
    const fechaFin = document.getElementById('fecha-fin');
    const filtrarBtn = document.getElementById('filtrar-ventas');
    
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
            // Para los selectores, asegurarse de que tengan un valor válido
            if (element.tagName === 'SELECT') {
                currentFilters[key] = element.value || element.options[0].value;
            } else {
                currentFilters[key] = element.value;
            }
        }
    }
}

async function loadVentasData() {
    if (!validateDates()) return;
    
    showLoading(true);
    
    try {
        const params = new URLSearchParams({
            ...currentFilters,
            tipoVenta: currentFilters.tipoVenta || 'TODAS',
            metodoPago: currentFilters.metodoPago || 'TODOS',
            sort: `${currentSort.field},${currentSort.direction}`
        });
        
        const response = await fetch(`/reportes/ventas/lista/data?${params.toString()}`);
        
        if (!response.ok) {
            throw new Error(`Error del servidor: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (data.error) {
            throw new Error(data.error);
        }
        
        updateVentasTable(data.content);
        updatePagination(data);
        // Solo mostrar notificación si hay datos
        if (data.content && data.content.length > 0) {
            showNotification('Lista de ventas actualizada', 'success');
        }
        
    } catch (error) {
        console.error('Error cargando ventas:', error);
        showNotification('Error al cargar las ventas: ' + error.message, 'error');
        showEmptyState('Error al cargar los datos');
    } finally {
        showLoading(false);
    }
}

function updateVentasTable(ventas) {
    const tbody = document.getElementById('ventas-list');
    if (!tbody) return;

    if (!ventas || ventas.length === 0) {
        showEmptyState();
        return;
    }

    tbody.innerHTML = '';
    ventas.forEach(venta => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${formatDate(venta.fechaVenta)}</td>
            <td>#${venta.id}</td>
            <td>${venta.cliente ? venta.cliente.nombre : 'Venta al contado'}</td>
            <td>${venta.tipoVenta}</td>
            <td>${venta.metodoPago}</td>
            <td>${formatCurrency(venta.totalVenta)}</td>
            <td>${formatCurrency(venta.totalVentaBs, true)}</td>
            <td>
                <div class="btn-group">
                    <button class="btn btn-sm btn-info" onclick="verDetalle(${venta.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="descargarPDF(${venta.id})">
                        <i class="fas fa-file-pdf"></i>
                    </button>
                    <button class="btn btn-sm btn-secondary" onclick="imprimirTicket(${venta.id})">
                        <i class="fas fa-print"></i>
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="confirmarAnularVenta(${venta.id})">
                        <i class="fas fa-ban"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function updatePagination(data) {
    totalPages = data.totalPages;
    
    // Actualizar información de paginación
    document.getElementById('current-page').textContent = currentFilters.page + 1;
    document.getElementById('total-pages').textContent = totalPages;
    
    // Habilitar/deshabilitar botones
    document.getElementById('prev-page').disabled = currentFilters.page === 0;
    document.getElementById('next-page').disabled = currentFilters.page >= totalPages - 1;
    
    // Actualizar contador de resultados
    document.getElementById('results-count').textContent = 
        `Mostrando ${data.numberOfElements} de ${data.totalElements} resultados`;
}

function updateSortIndicators() {
    document.querySelectorAll('.sortable').forEach(th => {
        const field = th.getAttribute('data-field');
        th.classList.remove('asc', 'desc');
        if (field === currentSort.field) {
            th.classList.add(currentSort.direction);
        }
    });
}

function showEmptyState(message = 'No se encontraron ventas para los filtros seleccionados') {
    const tbody = document.getElementById('ventas-list');
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="8" class="text-center empty-state">
                <div class="empty-state-message">
                    <i class="fas fa-search"></i>
                    <p>${message}</p>
                </div>
            </td>
        </tr>
    `;
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

function verDetalle(ventaId) {
    window.location.href = `/ventas/detalle/${ventaId}`;
}

async function descargarPDF(ventaId) {
    try {
        showLoading(true);
        const response = await fetch(`/reportes/ventas/pdf/${ventaId}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/pdf'
            }
        });

        if (!response.ok) {
            throw new Error('Error al generar el PDF');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `venta-${ventaId}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showNotification('PDF descargado exitosamente', 'success');
    } catch (error) {
        console.error('Error al descargar PDF:', error);
        showNotification('Error al descargar el PDF: ' + error.message, 'error');
    } finally {
        showLoading(false);
    }
}

async function imprimirTicket(ventaId) {
    try {
        showLoading(true);
        const response = await fetch(`/reportes/ventas/ticket/${ventaId}`, {
            method: 'GET',
            headers: {
                'Accept': 'application/pdf'
            }
        });

        if (!response.ok) {
            throw new Error('Error al generar el ticket');
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const printWindow = window.open(url, '_blank');
        printWindow.onload = function() {
            printWindow.print();
            window.URL.revokeObjectURL(url);
        };
        showNotification('Ticket generado exitosamente', 'success');
    } catch (error) {
        console.error('Error al imprimir ticket:', error);
        showNotification('Error al imprimir el ticket: ' + error.message, 'error');
    } finally {
        showLoading(false);
    }
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function formatCurrency(value, isBs = false) {
    if (isBs) {
        return Number(value).toFixed(2) + ' Bs';
    }
    return '$' + Number(value).toFixed(2);
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

function confirmarAnularVenta(ventaId) {
    // Crear modal de confirmación
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.id = 'modalAnularVenta';
    modal.innerHTML = `
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Confirmar Anulación</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>¿Está seguro que desea anular esta venta?</p>
                    <p>Esta acción no se puede deshacer.</p>
                    <div class="form-group mb-3">
                        <label for="motivoAnulacion">Motivo de la anulación:</label>
                        <textarea class="form-control" id="motivoAnulacion" rows="3" required></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-danger" onclick="anularVenta(${ventaId})">Anular</button>
                </div>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    
    // Inicializar modal de Bootstrap
    const modalInstance = new bootstrap.Modal(modal);
    modalInstance.show();
    
    // Limpiar modal cuando se cierre
    modal.addEventListener('hidden.bs.modal', () => {
        modal.remove();
    });
}

async function anularVenta(ventaId) {
    try {
        const motivo = document.getElementById('motivoAnulacion').value;
        if (!motivo) {
            showNotification('Debe proporcionar un motivo para la anulación', 'error');
            return;
        }

        // Obtener el token CSRF
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        // Configurar los headers
        const headers = {
            'Content-Type': 'application/json'
        };

        // Agregar el token CSRF si está disponible
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        // Mostrar indicador de carga
        showLoading(true);

        const response = await fetch(`/reportes/ventas/${ventaId}/anular`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify({ motivo: motivo })
        });

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.error || 'Error al anular la venta');
        }

        // Ocultar el modal y mostrar notificación de éxito
        const modalElement = document.getElementById('modalAnularVenta');
        if (modalElement) {
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) {
                modal.hide();
            }
        }
        showNotification('Venta anulada exitosamente', 'success');
        
        // Recargar la tabla
        loadVentasData();

    } catch (error) {
        console.error('Error al anular venta:', error);
        showNotification(error.message || 'Error al anular la venta', 'error');
    } finally {
        // Ocultar indicador de carga
        showLoading(false);
    }
} 