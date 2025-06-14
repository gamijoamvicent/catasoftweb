document.addEventListener('DOMContentLoaded', function() {
    // Referencias a elementos del DOM
    const dateRange = document.getElementById('dateRange');
    const customDateInputs = document.getElementById('customDateInputs');
    const startDate = document.getElementById('startDate');
    const endDate = document.getElementById('endDate');

    // Charts
    let ventasDiariasChart;
    let metodoPagoChart;
    let topCombosChart;

    // Event Listeners
    dateRange.addEventListener('change', function() {
        if (this.value === 'custom') {
            customDateInputs.style.display = 'block';
        } else {
            customDateInputs.style.display = 'none';
            cargarDatos(this.value);
        }
    });

    startDate.addEventListener('change', () => {
        if (endDate.value) cargarDatos('custom');
    });

    endDate.addEventListener('change', () => {
        if (startDate.value) cargarDatos('custom');
    });

    function cargarDatos(periodo) {
        let params = new URLSearchParams();
        
        if (periodo === 'custom') {
            params.append('startDate', startDate.value);
            params.append('endDate', endDate.value);
        } else {
            params.append('periodo', periodo);
        }

        fetch('/reportes/api/combos?' + params.toString())
            .then(response => response.json())
            .then(data => {
                actualizarEstadisticas(data);
                actualizarGraficos(data);
                actualizarTabla(data.ventas);
            })
            .catch(error => {
                console.error('Error al cargar datos:', error);
                mostrarError('Error al cargar los datos del reporte');
            });
    }

    function actualizarEstadisticas(data) {
        document.getElementById('totalVentas').textContent = data.totalVentas;
        document.getElementById('ingresosTotales').textContent = '$' + data.ingresosTotales.toFixed(2);
        document.getElementById('promedioVenta').textContent = '$' + data.promedioVenta.toFixed(2);
        document.getElementById('comboTopVendido').textContent = data.comboMasVendido;
    }

    function actualizarGraficos(data) {
        // Gráfico de Ventas Diarias
        if (ventasDiariasChart) {
            ventasDiariasChart.destroy();
        }
        const ctxVentas = document.getElementById('ventasDiarias').getContext('2d');
        ventasDiariasChart = new Chart(ctxVentas, {
            type: 'line',
            data: {
                labels: data.ventasPorDia.map(v => v.fecha),
                datasets: [{
                    label: 'Ventas por Día',
                    data: data.ventasPorDia.map(v => v.total),
                    borderColor: '#1976D2',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });

        // Gráfico de Métodos de Pago
        if (metodoPagoChart) {
            metodoPagoChart.destroy();
        }
        const ctxMetodoPago = document.getElementById('metodoPagoChart').getContext('2d');
        metodoPagoChart = new Chart(ctxMetodoPago, {
            type: 'doughnut',
            data: {
                labels: Object.keys(data.porMetodoPago),
                datasets: [{
                    data: Object.values(data.porMetodoPago),
                    backgroundColor: [
                        '#1565C0', '#1976D2', '#2196F3', '#64B5F6', '#90CAF9'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });

        // Gráfico de Top Combos
        if (topCombosChart) {
            topCombosChart.destroy();
        }
        const ctxTopCombos = document.getElementById('topCombosChart').getContext('2d');
        topCombosChart = new Chart(ctxTopCombos, {
            type: 'bar',
            data: {
                labels: data.topCombos.map(c => c.nombre),
                datasets: [{
                    label: 'Ventas',
                    data: data.topCombos.map(c => c.cantidad),
                    backgroundColor: '#1976D2'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    function actualizarTabla(ventas) {
        const tbody = document.getElementById('ventasTableBody');
        tbody.innerHTML = '';
        
        ventas.forEach(venta => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${new Date(venta.fechaVenta).toLocaleString()}</td>
                <td>${venta.combo.nombre}</td>
                <td>$${venta.valorVentaUSD}</td>
                <td>Bs. ${venta.valorVentaBS}</td>
                <td>${venta.metodoPago}</td>
                <td>${venta.tasaConversion}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    function mostrarError(mensaje) {
        // Implementar mostrar error al usuario
        alert(mensaje);
    }

    // Cargar datos iniciales
    cargarDatos('today');
});
