// Funcionalidad para el módulo de ventas
let productosDisponibles = []; // Array para almacenar todos los productos
let productosSeleccionados = []; // Array para el carrito
let tasas = {};

document.addEventListener('DOMContentLoaded', function() {
    cargarTasas(); // Cargar tasas primero
    initializeVentasModule();
    cargarProductos();
});

function initializeVentasModule() {
    // Configurar búsqueda
    const buscarField = document.getElementById('buscarField');
    if (buscarField) {
        buscarField.addEventListener('keyup', debounce(buscarProducto, 300));
    }

    // Configurar lista de sugerencias
    const sugerenciasList = document.getElementById('sugerenciasList');
    if (sugerenciasList) {
        // Remover cualquier evento click previo
        sugerenciasList.replaceWith(sugerenciasList.cloneNode(true));
        
        // Obtener la nueva referencia después del clonado
        const newSugerenciasList = document.getElementById('sugerenciasList');
        
        // Agregar el nuevo evento
        newSugerenciasList.addEventListener('click', function(event) {
            event.preventDefault(); // Prevenir comportamiento por defecto
            event.stopPropagation(); // Detener la propagación del evento
            
            const li = event.target.closest('li');
            if (li && !li.classList.contains('no-results')) {
                const productoId = li.getAttribute('data-id');
                const producto = productosDisponibles.find(p => p.id === parseInt(productoId));
                if (producto) {
                    agregarAlCarrito(producto);
                }
            }
        });
    }

    setupQuantityControls();
    setupPaymentCalculations();
    setupFormValidation();
}

// Cargar tasas desde el DOM
function cargarTasas() {
    tasas = {};
    document.querySelectorAll('.tasa-value').forEach(elem => {
        const tipo = elem.getAttribute('data-tasa');
        const valor = parseFloat(elem.getAttribute('data-valor'));
        if (tipo && !isNaN(valor)) {
            tasas[tipo] = valor;
            console.log(`Tasa cargada: ${tipo} = ${valor}`);
        }
    });
    
    // Verificar que al menos tengamos la tasa BCV
    if (!tasas['BCV']) {
        console.error('¡Advertencia! No se encontró la tasa BCV');
    }
    
    console.log('Tasas cargadas:', tasas);
}

// Cargar productos desde el DOM
function cargarProductos() {
    const dataProductos = document.querySelectorAll("#productosData li");
    productosDisponibles = Array.from(dataProductos).map(li => ({
        id: parseInt(li.getAttribute("data-id")),
        nombre: li.getAttribute("data-nombre") || "Producto sin nombre",
        codigoUnico: li.getAttribute("data-codigo") || "",
        precioVenta: parseFloat(li.getAttribute("data-precio")) || 0,
        cantidad: parseInt(li.getAttribute("data-cantidad")) || 0,
        marca: li.getAttribute("data-marca") || "Sin marca",
        tipoTasa: li.getAttribute("data-tipo-tasa") || "BCV"
    }));
    
    console.log('Productos cargados:', productosDisponibles);
}

// Búsqueda de productos
function buscarProducto() {
    const input = document.getElementById('buscarField');
    const valor = input.value.trim();
    
    if (!valor) {
        ocultarSugerencias();
        return;
    }

    console.log('Buscando productos con valor:', valor);

    // Realizar búsqueda en el servidor
    fetch(`/producto/buscar?q=${encodeURIComponent(valor)}`)
        .then(response => {
            console.log('Status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.text().then(text => {
                try {
                    return JSON.parse(text);
                } catch (e) {
                    console.log('Respuesta del servidor:', text);
                    throw new Error('La respuesta no es JSON válido');
                }
            });
        })
        .then(productos => {
            console.log('Productos encontrados:', productos);
            mostrarSugerencias(productos);
        })
        .catch(error => {
            console.error('Error buscando productos:', error);
            showNotification('Error al buscar productos', 'error');
        });
}

function mostrarSugerencias(productos) {
    const lista = document.getElementById('sugerenciasList');
    lista.innerHTML = '';
    
    if (!productos || productos.length === 0) {
        lista.innerHTML = '<li class="no-results">No se encontraron productos</li>';
        return;
    }

    productos.forEach(prod => {
        const li = document.createElement('li');
        li.setAttribute('data-id', prod.id);
        
        const stockClass = prod.cantidad === 0 ? 'stock-rojo' : 
                         prod.cantidad <= 10 ? 'stock-amarillo' : 'stock-verde';
        
        li.className = `producto-item ${stockClass}`;
        li.innerHTML = `
            <strong>${prod.nombre}</strong><br>
            <small>Código: ${prod.codigoUnico || 'Sin código'}</small><br>
            <small>Precio: $${prod.precioVenta.toFixed(2)} | Stock: ${prod.cantidad}</small>
        `;
        
        if (prod.cantidad === 0) {
            li.style.cursor = 'not-allowed';
        } else {
            li.style.cursor = 'pointer';
        }
        
        lista.appendChild(li);
    });
}

function ocultarSugerencias() {
    const lista = document.getElementById('sugerenciasList');
    lista.innerHTML = '';
}

// Función para generar ticket PDF
function generarTicketPdf() {
    if (productosSeleccionados.length === 0) {
        showNotification('No hay productos en el carrito', 'warning');
        return;
    }

    // Primero necesitamos confirmar la venta
    const ventaData = prepararDatosVenta();
    
    fetch('/ventas/confirmar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [document.querySelector('meta[name="_csrf_header"]').content]: 
            document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify(ventaData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        
        // Una vez confirmada la venta, generamos el PDF
        window.location.href = `/ventas/venta/${data.ventaId}/ticket-pdf`;
        showNotification('✅ Venta registrada exitosamente', 'success');
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification(error.message || 'Error al procesar la venta', 'error');
    });
}

// Función para confirmar la venta
function confirmarVenta() {
    if (productosSeleccionados.length === 0) {
        showNotification('No hay productos en el carrito', 'warning');
        return;
    }

    const ventaData = prepararDatosVenta();
    
    fetch('/ventas/confirmar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [document.querySelector('meta[name="_csrf_header"]').content]: 
            document.querySelector('meta[name="_csrf"]').content
        },
        body: JSON.stringify(ventaData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        
        showNotification('✅ Venta registrada exitosamente', 'success');
        
        // Imprimir el ticket inmediatamente después de confirmar la venta
        imprimirTicket(data.ventaId);
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification(error.message || 'Error al procesar la venta', 'error');
    });
}

// Función para imprimir ticket usando QZ Tray
function imprimirTicket(ventaId) {
    fetch(`/ventas/ticket/${ventaId}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                throw new Error(data.error);
            }

            if (typeof qz !== 'undefined') {
                qz.websocket.connect()
                .then(() => {
                    return qz.printers.find();
                })
                .then(printers => {
                    if (printers.length === 0) {
                        throw new Error('No se encontraron impresoras');
                    }
                    
                    // Usar la primera impresora encontrada o la configurada
                    const printer = data.impresora || printers[0];
                    const config = qz.configs.create(printer);
                    
                    // Usar el ticket ya formateado del backend
                    const lines = data.ticket.split('\n');
                    
                    return qz.print(config, lines);
                })
                .then(() => {
                    showNotification('Ticket impreso correctamente', 'success');
                    qz.websocket.disconnect();
                    
                    // Limpiar el carrito después de imprimir
                    productosSeleccionados = [];
                    actualizarTablaVentas();
                })
                .catch(error => {
                    console.error('Error de impresión:', error);
                    showNotification('Error al imprimir el ticket: ' + error.message, 'error');
                    qz.websocket.disconnect();
                });
            } else {
                console.error('QZ Tray no está disponible');
                showNotification('QZ Tray no está disponible. Por favor, instale QZ Tray para imprimir tickets.', 'error');
            }
        })
        .catch(error => {
            console.error('Error al obtener datos del ticket:', error);
            showNotification('Error al obtener datos del ticket: ' + error.message, 'error');
        });
}

// Función para preparar los datos del ticket
function prepararDatosTicket(ticketData) {
    const lines = [];
    
    // Encabezado
    lines.push('\x1B\x40'); // Inicializar impresora
    lines.push('\x1B\x61\x01'); // Centrar texto
    lines.push('CATASOFT');
    lines.push('\n');
    lines.push('TICKET DE VENTA');
    lines.push('\n');
    lines.push('Fecha: ' + new Date().toLocaleString());
    lines.push('\n\n');
    
    // Detalles de productos
    lines.push('\x1B\x61\x00'); // Alinear a la izquierda
    ticketData.items.forEach(item => {
        lines.push(`${item.cantidad}x ${item.nombre}`);
        lines.push(`  $${item.precioUnitario.toFixed(2)} c/u = $${(item.cantidad * item.precioUnitario).toFixed(2)}`);
        lines.push('\n');
    });
    
    // Totales
    lines.push('\n');
    lines.push('\x1B\x61\x02'); // Alinear a la derecha
    lines.push(`TOTAL USD: $${ticketData.totalUSD.toFixed(2)}`);
    lines.push(`TOTAL BS: ${ticketData.totalBS.toFixed(2)} Bs`);
    
    // Pie de ticket
    lines.push('\n\n');
    lines.push('\x1B\x61\x01'); // Centrar texto
    lines.push('¡Gracias por su compra!');
    lines.push('\n\n\n\n\n'); // Espacio para cortar
    lines.push('\x1B\x69'); // Cortar papel
    
    return lines;
}

// Función auxiliar para preparar los datos de la venta
function prepararDatosVenta() {
    const metodoPago = document.getElementById('metodoPago').value;
    const tipoVenta = document.getElementById('tipoVenta').value;
    const clienteId = document.getElementById('clienteId').value;

    // Mapear los productos seleccionados al formato que espera el servidor
    const items = productosSeleccionados.map(p => ({
        id: p.id,
        cantidad: p.cantidad,
        precioUnitario: p.precioVenta
    }));

    return {
        items,
        metodoPago,
        tipoVenta,
        clienteId: clienteId || null
    };
}

// Nueva función para agregar al carrito directamente
function agregarAlCarrito(producto) {
    console.log('Producto a agregar:', producto);
    console.log('Tasas disponibles:', tasas);

    if (producto.cantidad === 0) {
        showNotification('Este producto está agotado', 'error');
        return;
    }

    // Convertir el ID a número si es string
    const productoId = typeof producto.id === 'string' ? parseInt(producto.id) : producto.id;

    const index = productosSeleccionados.findIndex(p => p.id === productoId);
    if (index >= 0) {
        // Ya existe el producto, verificar stock antes de incrementar
        const nuevaCantidad = productosSeleccionados[index].cantidad + 1;
        if (nuevaCantidad > producto.cantidad) {
            showNotification(`Solo hay ${producto.cantidad} unidades disponibles`, 'warning');
            return;
        }
        productosSeleccionados[index].cantidad = nuevaCantidad;
    } else {
        // Producto nuevo, agregar con cantidad 1
        const productoConTasa = {
            ...producto,
            id: productoId,
            cantidad: 1,
            tipoTasa: producto.tipoTasa || 'BCV'
        };
        console.log('Agregando nuevo producto:', productoConTasa);
        productosSeleccionados.push(productoConTasa);
    }

    actualizarTablaVentas();
    ocultarSugerencias();
    document.getElementById('buscarField').value = '';
    showNotification('Producto agregado al carrito', 'success');
}

// Actualizar tabla de ventas
function actualizarTablaVentas() {
    const tbody = document.getElementById('ventasTableBody');
    tbody.innerHTML = '';
    
    let totalUSD = 0;
    let totalBs = 0;

    console.log('Actualizando tabla con productos:', productosSeleccionados);
    console.log('Tasas disponibles:', tasas);

    productosSeleccionados.forEach(prod => {
        // Asegurarse de que estamos usando la tasa correcta
        const tasaValor = tasas[prod.tipoTasa];
        if (!tasaValor) {
            console.error(`Tasa no encontrada para ${prod.tipoTasa}, usando BCV como respaldo`);
        }
        const tasaEfectiva = tasaValor || tasas['BCV'] || 0;
        
        console.log(`Usando tasa ${prod.tipoTasa}: ${tasaEfectiva} para producto ${prod.nombre}`);
        
        const precioBs = prod.precioVenta * tasaEfectiva;
        const subtotalUSD = prod.precioVenta * prod.cantidad;
        const subtotalBs = precioBs * prod.cantidad;
        
        console.log(`Cálculos para ${prod.nombre}:`, {
            precioUSD: prod.precioVenta,
            tasaUsada: tasaEfectiva,
            precioBs: precioBs,
            cantidad: prod.cantidad,
            subtotalUSD: subtotalUSD,
            subtotalBs: subtotalBs
        });

        totalUSD += subtotalUSD;
        totalBs += subtotalBs;

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="codigo-col">${prod.codigoUnico || 'Sin código'}</td>
            <td class="producto-col">${prod.nombre}</td>
            <td class="marca-col">${prod.marca || 'Sin marca'}</td>
            <td class="precio-col">$${prod.precioVenta.toFixed(2)}</td>
            <td class="precio-bs-col">
                ${precioBs.toFixed(2)} Bs
                <small class="tasa-badge">${prod.tipoTasa}</small>
            </td>
            <td class="cantidad-col">
                <input type="number" 
                       class="cantidad-input"
                       min="1" 
                       max="${prod.cantidad}" 
                       value="${prod.cantidad}" 
                       onchange="actualizarCantidad('${prod.id}', this.value)">
            </td>
            <td class="subtotal-col">
                <div class="subtotal-amounts">
                    <span>$${subtotalUSD.toFixed(2)}</span>
                    <span>${subtotalBs.toFixed(2)} Bs</span>
                </div>
            </td>
            <td class="acciones-col">
                <button onclick="eliminarProducto('${prod.id}')" 
                        class="btn btn-danger btn-sm">❌</button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    console.log('Totales calculados:', { totalUSD, totalBs });

    // Actualizar totales
    document.getElementById('totalUSD').textContent = `$${totalUSD.toFixed(2)}`;
    document.getElementById('totalBS').textContent = `${totalBs.toFixed(2)} Bs`;
}

// Actualizar cantidad
function actualizarCantidad(id, nuevaCantidad) {
    const producto = productosSeleccionados.find(p => p.id === id);
    if (!producto) return;
    
    fetch(`/productos/${id}`)
        .then(response => response.json())
        .then(productoActual => {
            const nuevaCant = parseInt(nuevaCantidad);
            
            if (nuevaCant <= 0) {
                showNotification('La cantidad debe ser mayor a cero', 'warning');
                actualizarTablaVentas();
                return;
            }
            
            if (nuevaCant > productoActual.cantidad) {
                showNotification(`Solo hay ${productoActual.cantidad} unidades disponibles`, 'warning');
                actualizarTablaVentas();
                return;
            }
            
            producto.cantidad = nuevaCant;
            actualizarTablaVentas();
        })
        .catch(error => {
            console.error('Error:', error);
            showNotification('Error al actualizar la cantidad', 'error');
            actualizarTablaVentas();
        });
}

// Eliminar producto
function eliminarProducto(id) {
    if (!confirm('¿Desea quitar este producto del carrito?')) return;
    
    productosSeleccionados = productosSeleccionados.filter(p => p.id !== id);
    actualizarTablaVentas();
}

// Limpiar carrito
function limpiarCarrito() {
    if (!confirm('¿Desea limpiar todo el carrito?')) return;
    
    productosSeleccionados = [];
    actualizarTablaVentas();
}

// Control de cantidades
function setupQuantityControls() {
    document.querySelectorAll('.quantity-control').forEach(control => {
        const input = control.querySelector('input');
        const decreaseBtn = control.querySelector('.decrease');
        const increaseBtn = control.querySelector('.increase');

        if (!input || !decreaseBtn || !increaseBtn) return;

        decreaseBtn.addEventListener('click', () => updateQuantity(input, -1));
        increaseBtn.addEventListener('click', () => updateQuantity(input, 1));
        input.addEventListener('change', () => validateQuantity(input));
    });
}

function updateQuantity(input, change) {
    const currentValue = parseInt(input.value) || 0;
    const newValue = Math.max(0, currentValue + change);
    input.value = newValue;
    input.dispatchEvent(new Event('change'));
}

// Cálculos de pago
function setupPaymentCalculations() {
    const montoInput = document.getElementById('monto-pagado');
    if (!montoInput) return;

    montoInput.addEventListener('input', function() {
        calculateChange();
    });
}

function calculateChange() {
    const totalVenta = parseFloat(document.getElementById('total-venta').textContent) || 0;
    const montoPagado = parseFloat(document.getElementById('monto-pagado').value) || 0;
    const cambio = montoPagado - totalVenta;

    updateChangeDisplay(cambio);
}

function updateChangeDisplay(cambio) {
    const cambioElement = document.getElementById('cambio');
    if (!cambioElement) return;

    cambioElement.textContent = cambio.toFixed(2);
    cambioElement.classList.toggle('text-success', cambio >= 0);
    cambioElement.classList.toggle('text-error', cambio < 0);
}

// Validación del formulario
function setupFormValidation() {
    const form = document.querySelector('.venta-form');
    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        if (validateVentaForm()) {
            submitVenta(this);
        }
    });
}

function validateVentaForm() {
    const items = document.querySelectorAll('.producto-item');
    if (items.length === 0) {
        showNotification('Debe agregar al menos un producto', 'warning');
        return false;
    }

    const montoPagado = parseFloat(document.getElementById('monto-pagado').value) || 0;
    const totalVenta = parseFloat(document.getElementById('total-venta').textContent) || 0;

    if (montoPagado < totalVenta) {
        showNotification('El monto pagado debe ser mayor o igual al total', 'warning');
        return false;
    }

    return true;
}

// Utilidades
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
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

// Funcionalidades para reportes
function initializeReportes() {
    setupDateRangePicker();
    setupCharts();
    setupExport();
}

function setupDateRangePicker() {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        input.addEventListener('change', function() {
            updateReporte();
        });
    });
}

function updateReporte() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;

    if (!fechaInicio || !fechaFin) return;

    fetch(`/api/reportes/ventas?inicio=${fechaInicio}&fin=${fechaFin}`)
        .then(response => response.json())
        .then(data => {
            updateEstadisticas(data);
            updateGraficos(data);
        })
        .catch(error => {
            console.error('Error actualizando reporte:', error);
            showNotification('Error al actualizar el reporte', 'error');
        });
}

// Exportación de reportes
function setupExport() {
    const exportBtn = document.querySelector('.export-btn');
    if (!exportBtn) return;

    exportBtn.addEventListener('click', function() {
        const fechaInicio = document.getElementById('fecha-inicio').value;
        const fechaFin = document.getElementById('fecha-fin').value;
        
        window.location.href = `/api/reportes/ventas/exportar?inicio=${fechaInicio}&fin=${fechaFin}`;
    });
}

// Agregar estilos dinámicamente
const style = document.createElement('style');
style.textContent = `
    .tasa-badge {
        display: inline-block;
        padding: 2px 6px;
        border-radius: 4px;
        background-color: #e9ecef;
        color: #495057;
        font-size: 0.75rem;
        margin-left: 4px;
    }
    
    .subtotal-amounts {
        display: flex;
        flex-direction: column;
        gap: 2px;
    }
    
    .total-amounts {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 4px;
    }
    
    .total-amounts span {
        font-weight: bold;
        font-size: 1.1em;
    }
    
    .cantidad-input {
        width: 70px;
        text-align: center;
    }
    
    .precio-bs-col {
        white-space: nowrap;
    }
`;
document.head.appendChild(style); 