document.addEventListener('DOMContentLoaded', function() {
    // Inicializar fechas por defecto (último mes)
    const today = new Date();
    const lastMonth = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());
    
    document.getElementById('fecha-inicio').value = lastMonth.toISOString().split('T')[0];
    document.getElementById('fecha-fin').value = today.toISOString().split('T')[0];

    // Cargar datos iniciales
    cargarDatos();

    // Event listeners
    document.getElementById('filtrar-reporte').addEventListener('click', cargarDatos);
    document.getElementById('exportar-excel').addEventListener('click', exportarExcel);
    document.getElementById('exportar-pdf').addEventListener('click', exportarPDF);
});

function cargarDatos() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;
    const tipoVenta = document.getElementById('tipo-venta').value;
    const metodoPago = document.getElementById('metodo-pago').value;

    // Mostrar indicador de carga
    mostrarCarga();

    // Realizar la petición al servidor
    fetch(`/api/reportes/ventas-cajas?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}&tipoVenta=${tipoVenta}&metodoPago=${metodoPago}`)
        .then(response => response.json())
        .then(data => {
            actualizarEstadisticas(data.estadisticas);
            actualizarGraficos(data.graficos);
            actualizarTabla(data.ventas);
        })
        .catch(error => {
            console.error('Error al cargar los datos:', error);
            mostrarError('Error al cargar los datos. Por favor, intente nuevamente.');
        })
        .finally(() => {
            ocultarCarga();
        });
}

function actualizarEstadisticas(estadisticas) {
    document.getElementById('total-ventas').textContent = estadisticas.totalVentas;
    document.getElementById('ingresos-totales').textContent = `$${estadisticas.ingresosDolares.toFixed(2)}`;
    document.getElementById('ingresos-totales-bs').textContent = `Bs ${estadisticas.ingresosBolivares.toFixed(2)}`;
    document.getElementById('cajas-vendidas').textContent = estadisticas.totalCajas;
}

function actualizarGraficos(datos) {
    // Gráfico de Ventas por Tipo de Caja
    const ctxTipoCaja = document.getElementById('tipoCajaChart').getContext('2d');
    new Chart(ctxTipoCaja, {
        type: 'pie',
        data: {
            labels: datos.tipoCaja.labels,
            datasets: [{
                data: datos.tipoCaja.valores,
                backgroundColor: [
                    '#1565c0',
                    '#1976D2',
                    '#00BFA5',
                    '#FFD600',
                    '#FF7043'
                ]
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });

    // Gráfico de Ventas por Método de Pago
    const ctxMetodoPago = document.getElementById('metodoPagoChart').getContext('2d');
    new Chart(ctxMetodoPago, {
        type: 'bar',
        data: {
            labels: datos.metodoPago.labels,
            datasets: [{
                label: 'Ventas por Método de Pago',
                data: datos.metodoPago.valores,
                backgroundColor: '#1565c0'
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function actualizarTabla(ventas) {
    const tbody = document.getElementById('tabla-ventas-cajas');
    tbody.innerHTML = '';

    ventas.forEach(venta => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${venta.id}</td>
            <td>${formatearFecha(venta.fecha)}</td>
            <td>${venta.cliente}</td>
            <td>${venta.tipoCaja}</td>
            <td>${venta.cantidad}</td>
            <td>$${venta.precioUnitario.toFixed(2)}</td>
            <td>$${venta.total.toFixed(2)}</td>
            <td>${venta.metodoPago}</td>
            <td><span class="badge ${venta.estado === 'COMPLETADA' ? 'badge-success' : 'badge-warning'}">${venta.estado}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function formatearFecha(fecha) {
    return new Date(fecha).toLocaleDateString('es-ES', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function exportarExcel() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;
    window.location.href = `/api/reportes/ventas-cajas/exportar/excel?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
}

function exportarPDF() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;
    window.location.href = `/api/reportes/ventas-cajas/exportar/pdf?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
}

function mostrarCarga() {
    // Implementar lógica para mostrar indicador de carga
}

function ocultarCarga() {
    // Implementar lógica para ocultar indicador de carga
}

function mostrarError(mensaje) {
    // Implementar lógica para mostrar mensaje de error
    alert(mensaje);
} 