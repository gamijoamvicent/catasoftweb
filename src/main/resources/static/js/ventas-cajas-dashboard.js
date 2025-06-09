// Variables para almacenar referencias a los gráficos

document.addEventListener('DOMContentLoaded', function() {
    // Destruir cualquier gráfico existente de sesiones anteriores
    destruirTodosLosGraficos();

    // Inicializar fecha por defecto (último mes)
    inicializarFechas();

    // Cargar datos iniciales
    cargarDashboard();

    // Event listeners
    document.getElementById('btn-filtrar').addEventListener('click', cargarDashboard);
    document.getElementById('btn-exportar').addEventListener('click', exportarDatos);
});

function destruirTodosLosGraficos() {
    // Ya no hay gráficos para destruir
}

function inicializarFechas() {
    const hoy = new Date();
    const mesAnterior = new Date();
    mesAnterior.setMonth(mesAnterior.getMonth() - 1);

    document.getElementById('fecha-fin').valueAsDate = hoy;
    document.getElementById('fecha-inicio').valueAsDate = mesAnterior;
}

function cargarDashboard() {
    // Destruir gráficos existentes antes de cargar nuevos datos
    destruirTodosLosGraficos();

    // Mostrar indicador de carga
    mostrarCargando(true);

    // Obtener parámetros de filtro
    const filtros = obtenerFiltros();

    // Realizar petición al servidor
    fetch(`/reportes/ventas-cajas/data?${filtros}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar los datos');
            }
            return response.json();
        })
        .then(data => {
            try {
                // Actualizar estadísticas
                actualizarEstadisticas(data.estadisticas || {});

                // Ya no hay gráficos para renderizar

                // Actualizar tabla
                actualizarTablaVentas(data.ultimasVentas || []);

                // Ocultar indicador de carga
                mostrarCargando(false);
            } catch (err) {
                console.error('Error al procesar datos:', err);
                mostrarError('Error al procesar los datos del dashboard');
                mostrarCargando(false);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarError('Error al cargar los datos del dashboard');
            mostrarCargando(false);
        });
}

function obtenerFiltros() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;
    const tipoCaja = document.getElementById('tipo-caja').value;

    return new URLSearchParams({
        fechaInicio: fechaInicio,
        fechaFin: fechaFin,
        tipoCaja: tipoCaja
    }).toString();
}

function actualizarEstadisticas(estadisticas) {
    document.getElementById('total-cajas').textContent = estadisticas.totalCajas;
    document.getElementById('total-ingresos').textContent = formatoDinero(estadisticas.totalIngresos);
    document.getElementById('caja-top').textContent = estadisticas.cajaMasVendida || '-';
    document.getElementById('caja-top-cantidad').textContent = 
        `${estadisticas.cajaMasVendidaCantidad || 0} unidades`;
    document.getElementById('promedio-venta').textContent = 
        formatoDinero(estadisticas.promedioVenta);
}




// Función de renderizado del gráfico de ingresos eliminada

function actualizarTablaVentas(ventas) {
    const tbody = document.getElementById('tabla-ventas-body');
    tbody.innerHTML = '';

    if (ventas.length === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td colspan="6" class="text-center">No hay datos para mostrar</td>`;
        tbody.appendChild(tr);
        return;
    }

    ventas.forEach(venta => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${formatoFecha(venta.fecha)}</td>
            <td>${venta.cajaNombre || venta.tipoCaja}</td>
            <td>${venta.cantidad}</td>
            <td>${formatoDinero(venta.precioUnitario)}</td>
            <td>${formatoDinero(venta.total)}</td>
            <td>
                <button class="btn btn-sm btn-info ver-detalle" data-id="${venta.id}" title="Ver detalle">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-danger eliminar-venta" data-id="${venta.id}" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    // Agregar event listeners a los botones de detalle
    document.querySelectorAll('.ver-detalle').forEach(btn => {
        btn.addEventListener('click', function() {
            const id = this.getAttribute('data-id');
            verDetalleVenta(id);
        });
    });

    // Agregar event listeners a los botones de eliminación
    document.querySelectorAll('.eliminar-venta').forEach(btn => {
        btn.addEventListener('click', function() {
            const id = this.getAttribute('data-id');
            eliminarVenta(id);
        });
    });
}

function verDetalleVenta(id) {
    // Implementación para ver detalles de una venta específica
    alert(`Mostrando detalles de la venta ID: ${id}`);
    // Aquí podría abrirse un modal con los detalles o redirigir a una página de detalles
}

function eliminarVenta(id) {
    if (confirm('¿Está seguro que desea eliminar esta venta de caja? Esta acción no se puede deshacer.')) {
        // Preparar los headers
        const headers = {
            'Content-Type': 'application/json'
        };

        // Intentar obtener el token CSRF si existe
        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        const csrfHeaderMeta = document.querySelector('meta[name="_csrf_header"]');

        if (csrfMeta && csrfHeaderMeta) {
            headers[csrfHeaderMeta.content] = csrfMeta.content;
        }

        // Realizar petición al servidor para inactivar la venta de caja
        fetch(`/api/ventas-cajas/${id}/inactivar`, {
            method: 'POST',
            headers: headers
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al eliminar la venta de caja');
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                alert('Venta de caja eliminada correctamente');
                // Recargar los datos del dashboard
                cargarDashboard();
            } else {
                throw new Error(data.message || 'Error al eliminar la venta de caja');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al eliminar la venta de caja: ' + error.message);
        });
    }
}

function exportarDatos() {
    const filtros = obtenerFiltros();
    const url = `/reportes/ventas-cajas/exportar?${filtros}`;
    window.location.href = url;
}

function mostrarCargando(mostrar) {
    // Implementación del indicador de carga
    // Por simplicidad, podría usarse una biblioteca como SweetAlert2
    if (mostrar) {
        console.log('Cargando datos...');
    }
}

function mostrarError(mensaje) {
    alert(mensaje);
}

// Funciones de utilidad
function formatoDinero(valor) {
    return '$' + parseFloat(valor).toFixed(2);
}

function formatoFecha(fecha) {
    const d = new Date(fecha);
    return d.toLocaleDateString('es-ES');
}
