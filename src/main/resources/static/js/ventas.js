// Variables globales
let productosDisponibles = []; // Array para almacenar todos los productos
let productosSeleccionados = []; // Array para el carrito
let tasas = {};

// Variables globales para la navegación
let selectedProductIndex = -1;
let searchResults = [];
let selectedTableRow = -1;
let isNavigatingTable = false;

// Inicialización de tokens CSRF
let csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
let csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

document.addEventListener('DOMContentLoaded', function() {
    // Inicializar tokens CSRF
    csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
    
    if (!csrfToken || !csrfHeader) {
        console.error('No se encontraron los tokens CSRF');
    }
    
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

        // Intentar obtener tasas del servidor si no están disponibles en el DOM
        fetch('/api/tasas/actuales')
            .then(response => response.json())
            .then(data => {
                if (data && Array.isArray(data) && data.length > 0) {
                    data.forEach(tasa => {
                        if (tasa.tipoTasa && tasa.valor) {
                            tasas[tasa.tipoTasa] = tasa.valor;
                            console.log(`Tasa cargada desde API: ${tasa.tipoTasa} = ${tasa.valor}`);
                        }
                    });
                    console.log('Tasas actualizadas desde API:', tasas);
                }
            })
            .catch(error => {
                console.error('Error al cargar tasas desde API:', error);

                // Establecer tasa BCV por defecto como fallback final
                if (!tasas['BCV']) {
                    console.warn('Estableciendo tasa BCV por defecto como fallback');
                    tasas['BCV'] = 40.0; // Valor de fallback
                }
            });
    }
    
    console.log('Tasas cargadas:', tasas);
}

// Cargar productos desde el DOM
function cargarProductos() {
    const dataProductos = document.querySelectorAll("#productosData li");
    productosDisponibles = Array.from(dataProductos).map(li => ({
        id: parseInt(li.getAttribute("data-id")),
        nombre: li.getAttribute("data-nombre") || "Producto sin nombre",
        // codigoUnico eliminado: li.getAttribute("data-codigo") || "",
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
    fetch(`/ventas/buscar?termino=${encodeURIComponent(valor)}`)
        .then(response => {
            console.log('Status:', response.status);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(productos => {
            console.log('Productos encontrados:', productos);
            if (!Array.isArray(productos)) {
                throw new Error('La respuesta no es un array de productos');
            }
            mostrarSugerencias(productos);
        })
        .catch(error => {
            console.error('Error buscando productos:', error);
            showNotification('Error al buscar productos: ' + error.message, 'error');
            ocultarSugerencias();
        });
}

function mostrarSugerencias(productos) {
    const lista = document.getElementById('sugerenciasList');
    lista.innerHTML = '';
    
    if (!productos || productos.length === 0) {
        lista.innerHTML = '<li class="no-results">No se encontraron productos</li>';
        return;
    }

    productos.forEach((prod, index) => {
        const li = document.createElement('li');
        li.setAttribute('data-id', prod.id);
        li.setAttribute('data-index', index);
        
        // Colores de stock más serios
        let stockClass = '';
        if (prod.cantidad === 0) {
            stockClass = 'stock-gray'; // gris para agotado
        } else if (prod.cantidad <= 10) {
            stockClass = 'stock-amber'; // ámbar para bajo stock
        } else {
            stockClass = 'stock-blue'; // azul para suficiente stock
        }
        
        li.className = `producto-item ${stockClass}`;
        li.innerHTML = `
            <strong>${prod.nombre}</strong><br>
            <small>Precio: $${prod.precioVenta.toFixed(2)} | Stock: ${prod.cantidad}</small>
        `;
        
        if (prod.cantidad === 0) {
            li.style.cursor = 'not-allowed';
            li.style.opacity = '0.7';
        } else {
            li.style.cursor = 'pointer';
            // Remover el evento onclick anterior si existe
            li.removeEventListener('click', handleProductClick);
            // Agregar el nuevo evento click
            li.addEventListener('click', handleProductClick);
        }
        
        lista.appendChild(li);
    });

    // Agregar estilos para mejorar la visibilidad
    const style = document.createElement('style');
    style.textContent = `
        .producto-item {
            padding: 10px;
            border-bottom: 1px solid #eee;
            transition: all 0.2s ease;
        }
        
        .producto-item:hover {
            background-color: #f5f5f5;
        }
        
        .producto-item.selected {
            background-color: #e3f2fd !important;
            border-left: 4px solid #1565c0;
        }
        
        .stock-gray {
            background-color: #f5f5f5 !important;
            color: #9e9e9e !important;
        }
        
        .stock-amber {
            background-color: #fff8e1 !important;
            color: #ff8f00 !important;
        }
        
        .stock-blue {
            background-color: #e3f2fd !important;
            color: #1565c0 !important;
        }
        
        .producto-item strong {
            display: block;
            margin-bottom: 4px;
        }
        
        .producto-item small {
            color: inherit;
            opacity: 0.8;
        }
    `;
    document.head.appendChild(style);
}

// Función para manejar el clic en un producto
function handleProductClick(event) {
    event.preventDefault();
    event.stopPropagation();
    
    const li = event.currentTarget;
    const productoId = li.getAttribute('data-id');
    const producto = productosDisponibles.find(p => p.id === parseInt(productoId));
    
    if (producto && producto.cantidad > 0) {
        agregarAlCarrito(producto);
        document.getElementById('buscarField').value = '';
        document.getElementById('buscarField').focus();
    }
}

function actualizarSeleccion(items) {
    if (!items || items.length === 0) return;
    
    Array.from(items).forEach((item, index) => {
        if (index === selectedProductIndex) {
            item.classList.add('selected');
            item.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        } else {
            item.classList.remove('selected');
        }
    });
}

document.addEventListener('keydown', function(e) {
    // Prevenir comportamiento por defecto de teclas de función
    if (['F1', 'F2', 'F3', 'F12'].includes(e.key)) {
        e.preventDefault();
        e.stopPropagation();
    }

    // Si estamos en la lista de sugerencias
    const sugerenciasList = document.getElementById('sugerenciasList');
    const items = sugerenciasList.getElementsByTagName('li');
    
    if (items.length > 0 && !isNavigatingTable) {
        switch(e.key) {
            case 'ArrowDown':
                e.preventDefault();
                selectedProductIndex = Math.min(selectedProductIndex + 1, items.length - 1);
                actualizarSeleccion(items);
                break;
            case 'ArrowUp':
                e.preventDefault();
                selectedProductIndex = Math.max(selectedProductIndex - 1, 0);
                actualizarSeleccion(items);
                break;
            case 'Enter':
                e.preventDefault();
                if (selectedProductIndex >= 0 && selectedProductIndex < items.length) {
                    const productoId = items[selectedProductIndex].getAttribute('data-id');
                    const producto = productosDisponibles.find(p => p.id === parseInt(productoId));
                    if (producto && producto.cantidad > 0) {
                        agregarAlCarrito(producto);
                        document.getElementById('buscarField').value = '';
                        document.getElementById('buscarField').focus();
                    } else {
                        showNotification('Este producto está agotado', 'warning');
                    }
                }
                break;
        }
    }

    // F1 para facturar
    if (e.key === 'F1') {
        e.preventDefault();
        e.stopPropagation();
        // Salir del modo de edición de tabla si está activo
        if (isNavigatingTable) {
            isNavigatingTable = false;
            selectedTableRow = -1;
            const rows = document.querySelectorAll('#ventasTableBody tr');
            rows.forEach(row => row.classList.remove('selected'));
        }
        confirmarVenta();
        return;
    }

    // F2 para enfocar el buscador
    if (e.key === 'F2') {
        e.preventDefault();
        e.stopPropagation();
        // Salir del modo de edición de tabla si está activo
        if (isNavigatingTable) {
            isNavigatingTable = false;
            selectedTableRow = -1;
            const rows = document.querySelectorAll('#ventasTableBody tr');
            rows.forEach(row => row.classList.remove('selected'));
        }
        // Enfocar el buscador
        const buscarField = document.getElementById('buscarField');
        if (buscarField) {
            buscarField.focus();
        }
        return;
    }

    // F3 para entrar en modo edición de tabla
    if (e.key === 'F3') {
        e.preventDefault();
        e.stopPropagation();
        isNavigatingTable = true;
        selectedTableRow = 0;
        const rows = document.querySelectorAll('#ventasTableBody tr');
        if (rows.length > 0) {
            actualizarSeleccionTabla(rows);
        }
        return;
    }

    // Si estamos en modo de edición de tabla
    if (isNavigatingTable) {
        const rows = document.querySelectorAll('#ventasTableBody tr');
        if (rows.length === 0) return;

        switch(e.key) {
            case 'ArrowDown':
                e.preventDefault();
                selectedTableRow = Math.min(selectedTableRow + 1, rows.length - 1);
                actualizarSeleccionTabla(rows);
                break;
            case 'ArrowUp':
                e.preventDefault();
                selectedTableRow = Math.max(selectedTableRow - 1, 0);
                actualizarSeleccionTabla(rows);
                break;
            case 'Enter':
                e.preventDefault();
                const selectedRow = rows[selectedTableRow];
                if (selectedRow) {
                    const input = selectedRow.querySelector('.cantidad-input');
                    if (input) {
                        // Si solo hay un item, salir del modo de edición
                        if (productosSeleccionados.length === 1) {
                            const productoId = selectedRow.getAttribute('data-id');
                            actualizarCantidad(productoId, input.value);
                            isNavigatingTable = false;
                            selectedTableRow = -1;
                            rows.forEach(row => row.classList.remove('selected'));
                            document.getElementById('buscarField').focus();
                        } else {
                            // Si hay más de un item, mantener el modo de edición
                            input.focus();
                            input.select();
                        }
                    }
                }
                break;
            case 'Escape':
                e.preventDefault();
                isNavigatingTable = false;
                selectedTableRow = -1;
                rows.forEach(row => row.classList.remove('selected'));
                break;
        }
    }
});

function actualizarSeleccionTabla(rows) {
    if (!rows || rows.length === 0) return;
    
    Array.from(rows).forEach((row, index) => {
        if (index === selectedTableRow) {
            row.classList.add('selected');
            row.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
            // Enfocar el input de cantidad
            const input = row.querySelector('.cantidad-input');
            if (input) {
                input.focus();
                input.select();
            }
        } else {
            row.classList.remove('selected');
        }
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
        showNotification('Debe agregar al menos un producto al carrito', 'warning');
        return;
    }

    // Verificar que tenemos los tokens CSRF
    if (!csrfToken || !csrfHeader) {
        showNotification('Error de seguridad: Tokens CSRF no encontrados', 'error');
        return;
    }

    // Verificar que todas las tasas estén cargadas
    let tasasFaltantes = false;
    const tiposTasas = new Set(productosSeleccionados.map(p => p.tipoTasa || 'BCV'));

    // Intentar recargar las tasas si faltan algunas
    if (Object.keys(tasas).length === 0) {
        console.log('No hay tasas cargadas, intentando recargar...');
        cargarTasas();
    }

    // Verificar cada tipo de tasa necesario
    tiposTasas.forEach(tipo => {
        if (!tasas[tipo]) {
            console.warn(`Falta tasa de tipo ${tipo}, se usará BCV como fallback`);
            tasasFaltantes = true;
        }
    });

    // Asegurar que al menos exista la tasa BCV
    if (tasasFaltantes || !tasas['BCV']) {
        console.warn('Estableciendo tasa BCV por defecto');
        tasas['BCV'] = 40.0;
    }

    // Verificar que todas las tasas necesarias estén cargadas
    const tiposTasaRequeridos = new Set(productosSeleccionados.map(p => p.tipoTasa || 'BCV'));
    let faltanTasas = false;

    tiposTasaRequeridos.forEach(tipo => {
        if (!tasas[tipo]) {
            console.error(`Falta la tasa de tipo: ${tipo}`);
            faltanTasas = true;
        }
    });

    if (faltanTasas) {
        console.log('Intentando recargar las tasas...');
        cargarTasas();

        // Verificar nuevamente después de recargar
        let todaviaFaltan = false;
        tiposTasaRequeridos.forEach(tipo => {
            if (!tasas[tipo]) {
                todaviaFaltan = true;
                // Asignar tasa BCV como fallback
                console.warn(`Usando tasa BCV como fallback para ${tipo}`);
                productosSeleccionados.forEach(p => {
                    if (p.tipoTasa === tipo) {
                        p.tipoTasa = 'BCV';
                    }
                });
            }
        });

        if (todaviaFaltan && !tasas['BCV']) {
            showNotification('Error: No hay tasas de cambio disponibles. No se puede procesar la venta.', 'error');
            return;
        }
    }

    const metodoPago = document.getElementById('metodoPago').value;
    const tipoVenta = document.getElementById('tipoVenta').value;
    const clienteId = document.getElementById('clienteId')?.value;

    // Validar que si es venta a crédito, tenga cliente seleccionado
    if (tipoVenta === 'CREDITO' && !clienteId) {
        showNotification('Debe seleccionar un cliente para ventas a crédito', 'warning');
        return;
    }

    const ventaData = {
        items: productosSeleccionados.map(p => ({
            id: p.id,
            cantidad: p.cantidad,
            precioUnitario: p.precioVenta
        })),
        metodoPago,
        tipoVenta: tipoVenta,
        clienteId: tipoVenta === 'CREDITO' ? clienteId : null
    };

    // Mostrar indicador de carga
    document.body.style.cursor = 'wait';

    const headers = {
        'Content-Type': 'application/json'
    };
    headers[csrfHeader] = csrfToken;

    fetch('/ventas/confirmar', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(ventaData)
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        showNotification('Venta realizada con éxito', 'success');
        
        // Limpiar el carrito después de una venta exitosa
        productosSeleccionados = [];
        actualizarTablaVentas();
        
        // Restablecer el tipo de venta a CONTADO
        document.getElementById('tipoVenta').value = 'CONTADO';
        
        // Ocultar selector de cliente si está visible
        const clienteSelector = document.getElementById('clienteSelector');
        if (clienteSelector) {
            clienteSelector.style.display = 'none';
        }
        
        // Limpiar campo de búsqueda de cliente
        const buscarClienteInput = document.getElementById('buscarCliente');
        if (buscarClienteInput) {
            buscarClienteInput.value = '';
        }
        
        // Imprimir ticket si está configurado
        if (data.ventaId) {
            imprimirTicket(data.ventaId);
        }


    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Error al procesar la venta: ' + error.message, 'error');
    })
    .finally(() => {
        document.body.style.cursor = 'default';
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

    // Validar que todos los productos tengan un tipo de tasa válido y asegurar que el valor existe
    productosSeleccionados.forEach(p => {
        // Si no tiene tipo de tasa o el tipo no existe en las tasas disponibles
        if (!p.tipoTasa || !tasas[p.tipoTasa]) {
            console.warn(`Producto ${p.nombre} (ID: ${p.id}) tiene tipo de tasa inválida: ${p.tipoTasa}. Cambiando a BCV.`);
            p.tipoTasa = 'BCV';
        }

        // Verificar si la tasa seleccionada existe
        if (!tasas[p.tipoTasa]) {
            console.warn(`No existe valor para la tasa ${p.tipoTasa}, cambiando a BCV para el producto ${p.nombre}`);
            p.tipoTasa = 'BCV';

            // Si tampoco hay tasa BCV, crear una por defecto
            if (!tasas['BCV']) {
                console.warn('No existe valor para la tasa BCV, estableciendo valor por defecto');
                tasas['BCV'] = 40.0;
            }
        }
    });

    // Mapear los productos seleccionados al formato que espera el servidor
    const items = productosSeleccionados.map(p => {
        // Verificar nuevamente el tipo de tasa
        let tipoTasaFinal = p.tipoTasa || 'BCV';

        // Si la tasa no está disponible, usar BCV
        if (!tasas[tipoTasaFinal]) {
            console.warn(`Usando tasa BCV para producto ${p.nombre} porque ${tipoTasaFinal} no está disponible`);
            tipoTasaFinal = 'BCV';
        }

        // Si tampoco hay tasa BCV, crearla
        if (!tasas['BCV']) {
            console.warn('Creando tasa BCV por defecto (40.0)');
            tasas['BCV'] = 40.0;
        }

        return {
            id: p.id,
            cantidad: p.cantidad,
            precioUnitario: p.precioVenta,
            tipoTasa: tipoTasaFinal
        };
    });

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

    // Verificar si hay tasas cargadas
    if (Object.keys(tasas).length === 0) {
        console.warn('No hay tasas cargadas. Recargando tasas...');
        cargarTasas();
    }

    if (producto.cantidad === 0) {
        showNotification('Este producto está agotado', 'error');
        return;
    }

    // Convertir el ID a número si es string
    const productoId = typeof producto.id === 'string' ? parseInt(producto.id) : producto.id;

    // Verificar que el tipo de tasa del producto sea válido
    const tipoTasa = producto.tipoTasa || 'BCV';
    if (!tasas[tipoTasa]) {
        console.warn(`El producto tiene un tipo de tasa no disponible: ${tipoTasa}. Usando BCV como fallback.`);
        producto.tipoTasa = 'BCV';
    }

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

// Alias para mantener compatibilidad con código existente
// NOTA: Esta función se usa en múltiples vistas. Si se necesita en venta-cajas.html, asegurarse de que el script ventas.js esté importado.
function actualizarTablaItems() {
    actualizarTablaVentas();
}
    
    let totalUSD = 0;
    let totalBs = 0;

    // Verificar si las tasas se han cargado correctamente
    if (Object.keys(tasas).length === 0) {
        console.error('Las tasas no están disponibles. Recargando tasas...');
        cargarTasas();

        // Establecer tasa BCV por defecto como fallback inmediato
        if (Object.keys(tasas).length === 0) {
            console.warn('Estableciendo tasa BCV por defecto como fallback inmediato');
            tasas['BCV'] = 40.0;
        }
    }

    console.log('Actualizando tabla con productos:', productosSeleccionados);
    console.log('Tasas disponibles:', tasas);

    productosSeleccionados.forEach(prod => {
        // Asegurar que el tipo de tasa sea válido, si no, usar BCV como fallback
        if (!prod.tipoTasa || !tasas[prod.tipoTasa]) {
            console.warn(`Tipo de tasa inválido o no disponible para producto ${prod.nombre}: ${prod.tipoTasa}. Usando BCV como fallback.`);
            prod.tipoTasa = 'BCV';
        }

        const tasaValor = tasas[prod.tipoTasa];
        const tasaEfectiva = tasaValor || tasas['BCV'] || 0;

        if (tasaEfectiva === 0) {
            console.error('¡Error crítico! No se pudo obtener una tasa de cambio válida.');
            showNotification('Error: No hay tasas de cambio disponibles', 'error');
        }
        
        const precioBs = prod.precioVenta * tasaEfectiva;
        const subtotalUSD = prod.precioVenta * prod.cantidad;
        const subtotalBs = precioBs * prod.cantidad;

        totalUSD += subtotalUSD;
        totalBs += subtotalBs;

        // Obtener el stock disponible del producto
        const productoDisponible = productosDisponibles.find(p => p.id === prod.id);
        const stockDisponible = productoDisponible ? productoDisponible.cantidad : 0;

        const tr = document.createElement('tr');
        tr.setAttribute('data-id', prod.id);
        tr.innerHTML = `
            <!-- <td class="codigo-col">${prod.codigoUnico || 'Sin código'}</td> -->
            <td class="producto-col">${prod.nombre}</td>
            <td class="marca-col">${prod.marca || 'Sin marca'}</td>
            <td class="precio-col">$${prod.precioVenta.toFixed(2)}</td>
            <td class="precio-bs-col">
                ${precioBs.toFixed(2)} Bs
                <small class="tasa-badge">${prod.tipoTasa}</small>
            </td>
            <td class="cantidad-col">
                <div class="quantity-control">
                    <button class="decrease">-</button>
                    <input type="number" 
                           class="cantidad-input"
                           min="1" 
                           max="${stockDisponible}" 
                           value="${prod.cantidad}">
                    <button class="increase">+</button>
                </div>
            </td>
            <td class="subtotal-col">
                <div class="subtotal-amounts">
                    <span>$${subtotalUSD.toFixed(2)}</span>
                    <span>${subtotalBs.toFixed(2)} Bs</span>
                </div>
            </td>
            <td class="acciones-col">
                <button onclick="eliminarProducto(${prod.id})" 
                        class="btn btn-danger btn-sm">❌</button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    console.log('Totales calculados:', { totalUSD, totalBs });

    // Actualizar totales
    document.getElementById('totalUSD').textContent = `$${totalUSD.toFixed(2)}`;
    document.getElementById('totalBS').textContent = `${totalBs.toFixed(2)} Bs`;

    // Reinicializar los controles de cantidad
    setupQuantityControls();
}

// Actualizar cantidad
function actualizarCantidad(id, nuevaCantidad) {
    const producto = productosSeleccionados.find(p => p.id === parseInt(id));
    if (!producto) return;
    
    // Obtener el stock del producto desde productosDisponibles
    const productoActual = productosDisponibles.find(p => p.id === parseInt(id));
    if (!productoActual) {
        showNotification('Error: Producto no encontrado', 'error');
        actualizarTablaVentas();
        return;
    }

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
}

// Eliminar producto
function eliminarProducto(id) {
    if (!confirm('¿Desea quitar este producto del carrito?')) return;
    
    // Convertir el ID a número si es string
    const productoId = typeof id === 'string' ? parseInt(id) : id;
    
    // Filtrar el producto del array
    productosSeleccionados = productosSeleccionados.filter(p => p.id !== productoId);
    
    // Actualizar la tabla
    actualizarTablaVentas();
    
    // Mostrar notificación
    showNotification('Producto eliminado del carrito', 'success');
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

        decreaseBtn.addEventListener('click', () => {
            const currentValue = parseInt(input.value) || 0;
            if (currentValue > 1) {
                input.value = currentValue - 1;
                const productoId = input.closest('tr').getAttribute('data-id');
                actualizarCantidad(productoId, input.value);
            }
        });

        increaseBtn.addEventListener('click', () => {
            const currentValue = parseInt(input.value) || 0;
            const productoId = input.closest('tr').getAttribute('data-id');
            const producto = productosDisponibles.find(p => p.id === parseInt(productoId));
            
            if (producto && currentValue < producto.cantidad) {
                input.value = currentValue + 1;
                actualizarCantidad(productoId, input.value);
            } else {
                showNotification(`Solo hay ${producto?.cantidad || 0} unidades disponibles`, 'warning');
            }
        });

        input.addEventListener('change', () => {
            const productoId = input.closest('tr').getAttribute('data-id');
            actualizarCantidad(productoId, input.value);
        });

        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                const productoId = input.closest('tr').getAttribute('data-id');
                actualizarCantidad(productoId, input.value);
                
                // Si solo hay un item, salir del modo de edición y enfocar el buscador
                if (productosSeleccionados.length === 1) {
                    isNavigatingTable = false;
                    selectedTableRow = -1;
                    const rows = document.querySelectorAll('#ventasTableBody tr');
                    rows.forEach(row => row.classList.remove('selected'));
                    document.getElementById('buscarField').focus();
                }
            }
        });
    });
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
    setupFilters();

    // Inicializar con el mes actual
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    
    document.getElementById('fecha-inicio').valueAsDate = firstDay;
    document.getElementById('fecha-fin').valueAsDate = today;
    
    // Cargar datos iniciales
    updateReporte();
}

function setupDateRangePicker() {
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        input.addEventListener('change', function() {
            updateReporte();
        });
    });
}

function setupFilters() {
    const tipoVenta = document.getElementById('tipo-venta');
    const metodoPago = document.getElementById('metodo-pago');
    const fechaInicio = document.getElementById('fecha-inicio');
    const fechaFin = document.getElementById('fecha-fin');
    const filtrarBtn = document.getElementById('filtrar-reporte');

    // Remover los event listeners automáticos
    [tipoVenta, metodoPago, fechaInicio, fechaFin].forEach(filtro => {
        if (filtro) {
            // Remover los event listeners anteriores
            const nuevoFiltro = filtro.cloneNode(true);
            filtro.parentNode.replaceChild(nuevoFiltro, filtro);
        }
    });

    // Configurar el botón de filtrar
    if (filtrarBtn) {
        filtrarBtn.addEventListener('click', () => {
            console.log('Aplicando filtros...');
            updateReporte();
        });
    }

    // Validación de fechas
    if (fechaInicio && fechaFin) {
        fechaInicio.addEventListener('change', validarFechas);
        fechaFin.addEventListener('change', validarFechas);
    }
}

function validarFechas() {
    const fechaInicio = document.getElementById('fecha-inicio');
    const fechaFin = document.getElementById('fecha-fin');
    const filtrarBtn = document.getElementById('filtrar-reporte');
    
    if (fechaInicio && fechaFin && filtrarBtn) {
        const inicio = new Date(fechaInicio.value);
        const fin = new Date(fechaFin.value);
        const hoy = new Date();
        
        // Resetear estilos
        fechaInicio.classList.remove('is-invalid');
        fechaFin.classList.remove('is-invalid');
        
        let esValido = true;
        
        // Validar que la fecha de inicio no sea posterior a la fecha fin
        if (inicio > fin) {
            fechaInicio.classList.add('is-invalid');
            fechaFin.classList.add('is-invalid');
            showNotification('La fecha de inicio no puede ser posterior a la fecha fin', 'warning');
            esValido = false;
        }
        
        // Validar que las fechas no sean futuras
        if (inicio > hoy || fin > hoy) {
            if (inicio > hoy) fechaInicio.classList.add('is-invalid');
            if (fin > hoy) fechaFin.classList.add('is-invalid');
            showNotification('No se pueden seleccionar fechas futuras', 'warning');
            esValido = false;
        }
        
        // Habilitar o deshabilitar el botón de filtrar
        filtrarBtn.disabled = !esValido;
    }
}

// Agregar estilos para la validación
const validationStyles = document.createElement('style');
validationStyles.textContent = `
    .is-invalid {
        border-color: #dc3545;
        background-color: #fff8f8;
    }
    
    .is-invalid:focus {
        border-color: #dc3545;
        box-shadow: 0 0 0 0.2rem rgba(220, 53, 69, 0.25);
    }
`;
document.head.appendChild(validationStyles);

function updateReporte() {
    const fechaInicio = document.getElementById('fecha-inicio').value;
    const fechaFin = document.getElementById('fecha-fin').value;
    const tipoVenta = document.getElementById('tipo-venta').value;
    const metodoPago = document.getElementById('metodo-pago').value;

    if (!fechaInicio || !fechaFin) {
        showNotification('Seleccione un rango de fechas válido', 'warning');
        return;
    }

    console.log('Aplicando filtros:', { fechaInicio, fechaFin, tipoVenta, metodoPago });

    // Mostrar indicador de carga
    document.body.style.cursor = 'wait';
    const loadingOverlay = document.createElement('div');
    loadingOverlay.className = 'loading-overlay';
    loadingOverlay.innerHTML = '<div class="spinner"></div>';
    document.body.appendChild(loadingOverlay);

    // Limpiar datos actuales
    mostrarEstadoVacio();

    const params = new URLSearchParams({
        fechaInicio: fechaInicio,
        fechaFin: fechaFin,
        tipoVenta: tipoVenta || '',
        metodoPago: metodoPago || ''
    });

    fetch(`/reportes/ventas/data?${params.toString()}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error del servidor: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Datos recibidos:', data);
            if (data.error) {
                throw new Error(data.error);
            }

            if (isDatosVacios(data)) {
                mostrarEstadoVacio('No se encontraron datos para los filtros seleccionados');
                return;
            }

            // Actualizar todos los componentes
            updateEstadisticas(data);
            updateGraficos(data);
            updateTablaVentas(data.ventas || []);
            
            // Actualizar URL con los filtros
            const newUrl = new URL(window.location.href);
            params.forEach((value, key) => {
                newUrl.searchParams.set(key, value);
            });
            window.history.pushState({}, '', newUrl);

            showNotification('Reporte actualizado correctamente', 'success');
        })
        .catch(error => {
            console.error('Error actualizando reporte:', error);
            showNotification('Error al actualizar el reporte: ' + error.message, 'error');
            mostrarEstadoVacio('Error al cargar los datos');
        })
        .finally(() => {
            document.body.style.cursor = 'default';
            const overlay = document.querySelector('.loading-overlay');
            if (overlay) overlay.remove();
        });
}

function isDatosVacios(data) {
    return (!data.ventas || data.ventas.length === 0) &&
           (!data.totalVentas || data.totalVentas === 0) &&
           (!data.totalIngresos || data.totalIngresos === 0);
}

function mostrarEstadoVacio(mensaje = 'No hay datos disponibles') {
    // Limpiar estadísticas
    document.getElementById('total-ventas').textContent = '0';
    document.getElementById('ingresos-totales').textContent = '$0.00';
    document.getElementById('ticket-promedio').textContent = '$0.00';
    document.getElementById('productos-vendidos').textContent = '0';

    // Limpiar gráficos
    const ctxMetodos = document.getElementById('metodosPagoChart');
    const ctxCreditos = document.getElementById('creditosChart');

    // Función auxiliar para mostrar mensaje en canvas
    const mostrarMensajeEnCanvas = (canvas) => {
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        // Limpiar el canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        // Configurar estilo del texto
        ctx.font = '14px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillStyle = '#666';
        // Dibujar el mensaje
        ctx.fillText(mensaje, canvas.width / 2, canvas.height / 2);
    };

    // Destruir y limpiar gráfico de métodos de pago
    if (ctxMetodos) {
        if (window.metodosPagoChart && typeof window.metodosPagoChart.destroy === 'function') {
            window.metodosPagoChart.destroy();
        }
        window.metodosPagoChart = null;
        mostrarMensajeEnCanvas(ctxMetodos);
    }

    // Destruir y limpiar gráfico de créditos
    if (ctxCreditos) {
        if (window.creditosChart && typeof window.creditosChart.destroy === 'function') {
            window.creditosChart.destroy();
        }
        window.creditosChart = null;
        mostrarMensajeEnCanvas(ctxCreditos);
    }

    // Limpiar tabla
    const tbody = document.getElementById('ventas-list');
    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" class="text-center empty-state">
                    <div class="empty-state-message">
                        <i class="fas fa-search"></i>
                        <p>${mensaje}</p>
                    </div>
                </td>
            </tr>
        `;
    }
}

// Agregar estilos para el estado vacío
const emptyStateStyles = document.createElement('style');
emptyStateStyles.textContent = `
    .empty-state {
        padding: 40px !important;
        text-align: center;
        color: #666;
    }

    .empty-state-message {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
    }

    .empty-state-message i {
        font-size: 24px;
        color: #999;
    }

    .empty-state-message p {
        margin: 0;
        font-size: 14px;
    }
`;
document.head.appendChild(emptyStateStyles);

function updateEstadisticas(data) {
    // Actualizar cards de estadísticas
    document.getElementById('total-ventas').textContent = data.totalVentas || 0;
    document.getElementById('ingresos-totales').textContent = formatCurrency(data.totalIngresos || 0);
    document.getElementById('ticket-promedio').textContent = formatCurrency(data.ticketPromedio || 0);
    document.getElementById('productos-vendidos').textContent = data.productosVendidos || 0;
}

function updateGraficos(data) {
    console.log('Actualizando gráficos con datos:', data);
    
    // Verificar si hay datos para los gráficos
    const hayDatosMetodosPago = data.ventasPorMetodoPago && Object.keys(data.ventasPorMetodoPago).length > 0;
    const hayDatosCreditos = data.estadisticasCreditos && Object.values(data.estadisticasCreditos).some(val => val > 0);

    // Si no hay datos para ningún gráfico, mostrar estado vacío
    if (!hayDatosMetodosPago && !hayDatosCreditos) {
        mostrarEstadoVacio('No hay datos para mostrar en los gráficos');
        return;
    }

    // Actualizar gráfico de métodos de pago si hay datos
    if (hayDatosMetodosPago) {
        updateMetodosPagoChart(data.ventasPorMetodoPago || {});
    } else {
        const ctxMetodos = document.getElementById('metodosPagoChart');
        if (window.metodosPagoChart && typeof window.metodosPagoChart.destroy === 'function') {
            window.metodosPagoChart.destroy();
            window.metodosPagoChart = null;
        }
        if (ctxMetodos) {
            const ctx = ctxMetodos.getContext('2d');
            ctx.clearRect(0, 0, ctxMetodos.width, ctxMetodos.height);
            ctx.font = '14px Arial';
            ctx.textAlign = 'center';
            ctx.fillStyle = '#666';
            ctx.fillText('No hay datos de métodos de pago', ctxMetodos.width / 2, ctxMetodos.height / 2);
        }
    }

    // Actualizar gráfico de créditos si hay datos
    if (hayDatosCreditos) {
        updateCreditosChart(data.estadisticasCreditos || {});
    } else {
        const ctxCreditos = document.getElementById('creditosChart');
        if (window.creditosChart && typeof window.creditosChart.destroy === 'function') {
            window.creditosChart.destroy();
            window.creditosChart = null;
        }
        if (ctxCreditos) {
            const ctx = ctxCreditos.getContext('2d');
            ctx.clearRect(0, 0, ctxCreditos.width, ctxCreditos.height);
            ctx.font = '14px Arial';
            ctx.textAlign = 'center';
            ctx.fillStyle = '#666';
            ctx.fillText('No hay datos de créditos', ctxCreditos.width / 2, ctxCreditos.height / 2);
        }
    }
}

function updateMetodosPagoChart(data) {
    const ctx = document.getElementById('metodosPagoChart');
    if (!ctx) {
        console.error('No se encontró el elemento canvas para el gráfico de métodos de pago');
        return;
    }

    console.log('Actualizando gráfico de métodos de pago con datos:', data);

    // Destruir el gráfico existente si existe
    if (window.metodosPagoChart && typeof window.metodosPagoChart.destroy === 'function') {
        window.metodosPagoChart.destroy();
    }

    const labels = Object.keys(data);
    const valores = Object.values(data);
    const total = valores.reduce((a, b) => a + b, 0);
    const porcentajes = valores.map(v => ((v / total) * 100).toFixed(1));

    // Colores modernos y atractivos con transparencia
    const colores = [
        'rgba(21, 101, 192, 0.8)',     // Azul principal
        'rgba(0, 191, 165, 0.8)',      // Verde acento
        'rgba(25, 118, 210, 0.8)',     // Azul secundario
        'rgba(13, 71, 161, 0.8)',      // Azul oscuro
        'rgba(0, 121, 107, 0.8)'       // Verde oscuro
    ];

    const bordeColores = [
        'rgb(21, 101, 192)',     // Bordes sólidos
        'rgb(0, 191, 165)',
        'rgb(25, 118, 210)',
        'rgb(13, 71, 161)',
        'rgb(0, 121, 107)'
    ];

    window.metodosPagoChart = new Chart(ctx, {
        type: 'doughnut', // Cambiado de 'pie' a 'doughnut' para un aspecto más moderno
        data: {
            labels: labels,
            datasets: [{
                data: valores,
                backgroundColor: colores,
                borderColor: bordeColores,
                borderWidth: 2,
                hoverOffset: 15,
                borderRadius: 5,
                spacing: 5
            }]
        },
        options: {
            responsive: true,
            cutout: '60%', // Tamaño del agujero en el centro
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        padding: 20,
                        usePointStyle: true,
                        pointStyle: 'circle',
                        font: {
                            size: 12,
                            family: "'Segoe UI', 'Arial', sans-serif"
                        }
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(255, 255, 255, 0.9)',
                    titleColor: '#333',
                    titleFont: {
                        size: 14,
                        weight: 'bold'
                    },
                    bodyColor: '#666',
                    bodyFont: {
                        size: 13
                    },
                    borderColor: '#ddd',
                    borderWidth: 1,
                    padding: 12,
                    boxPadding: 6,
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const percentage = porcentajes[context.dataIndex];
                            return ` ${label}: $${value.toFixed(2)} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true
            }
        }
    });
}

function updateCreditosChart(data) {
    const ctx = document.getElementById('creditosChart');
    if (!ctx) {
        console.error('No se encontró el elemento canvas para el gráfico de créditos');
        return;
    }

    console.log('Actualizando gráfico de créditos con datos:', data);

    // Destruir el gráfico existente si existe
    if (window.creditosChart && typeof window.creditosChart.destroy === 'function') {
        window.creditosChart.destroy();
    }

    // Preparar los datos para el gráfico
    const labels = ['Total Créditos', 'Total Pagado', 'Total Pendiente', 'Total Vencido'];
    const valores = [
        data['Total Créditos'] || 0,
        data['Total Pagado'] || 0,
        data['Total Pendiente'] || 0,
        data['Total Vencido'] || 0
    ];

    // Colores para cada sección
    const colores = [
        'rgba(21, 101, 192, 0.8)',  // Azul para Total Créditos
        'rgba(0, 191, 165, 0.8)',   // Verde para Total Pagado
        'rgba(255, 152, 0, 0.8)',   // Naranja para Total Pendiente
        'rgba(244, 67, 54, 0.8)'    // Rojo para Total Vencido
    ];

    window.creditosChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Monto en USD',
                data: valores,
                backgroundColor: colores,
                borderColor: colores.map(color => color.replace('0.8', '1')),
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return '$' + value.toFixed(2);
                        }
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return '$' + context.raw.toFixed(2);
                        }
                    }
                },
                title: {
                    display: true,
                    text: 'Estado de Créditos',
                    font: {
                        size: 16,
                        weight: 'bold'
                    }
                }
            }
        }
    });
}

function updateTablaVentas(ventas) {
    const tbody = document.getElementById('ventas-list');
    if (!tbody) return;

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
                <button class="btn btn-sm btn-info" onclick="verDetalle(${venta.id})">
                    <span class="icon">👁️</span>
                </button>
                <button class="btn btn-sm btn-secondary" onclick="imprimirTicket(${venta.id})">
                    <span class="icon">🖨️</span>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Exportación de reportes
function setupExport() {
    const exportButtons = document.querySelectorAll('.export-btn');
    if (!exportButtons) return;

    exportButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const format = this.getAttribute('data-format');
            const fechaInicio = document.getElementById('fecha-inicio').value;
            const fechaFin = document.getElementById('fecha-fin').value;
            const tipoVenta = document.getElementById('tipo-venta').value;
            const metodoPago = document.getElementById('metodo-pago').value;

            const params = new URLSearchParams({
                fechaInicio: fechaInicio,
                fechaFin: fechaFin,
                tipoVenta: tipoVenta,
                metodoPago: metodoPago
            });
            
            window.location.href = `/reportes/ventas/exportar/${format}?${params.toString()}`;
        });
    });
}

function formatCurrency(value, isBs = false) {
    if (isBs) {
        return Number(value).toFixed(2) + ' Bs';
    }
    return '$' + Number(value).toFixed(2);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function verDetalle(ventaId) {
    window.location.href = `/ventas/detalle/${ventaId}`;
}

// Función para desactivar una venta de caja
function desactivarVentaCaja(ventaCajaId) {
    if (!confirm('¿Está seguro que desea eliminar esta venta? Esta acción no se puede deshacer.')) {
        return;
    }

    // Verificar que tenemos los tokens CSRF
    if (!csrfToken || !csrfHeader) {
        showNotification('Error de seguridad: Tokens CSRF no encontrados', 'error');
        return;
    }

    // Mostrar indicador de carga
    document.body.style.cursor = 'wait';

    const headers = {
        'Content-Type': 'application/json'
    };
    headers[csrfHeader] = csrfToken;

    fetch(`/ventas/cajas/desactivar/${ventaCajaId}`, {
        method: 'POST',
        headers: headers
    })
    .then(response => response.json())
    .then(data => {
        if (data.error) {
            throw new Error(data.error);
        }
        showNotification('Venta eliminada con éxito', 'success');

        // Recargar la página para reflejar los cambios
        setTimeout(() => {
            location.reload();
        }, 1000);
    })
    .catch(error => {
        console.error('Error:', error);
        showNotification('Error al eliminar la venta: ' + error.message, 'error');
    })
    .finally(() => {
        document.body.style.cursor = 'default';
    });
}

function setupCharts() {
    // Asegurarse de que Chart.js esté disponible
    if (typeof Chart === 'undefined') {
        console.error('Chart.js no está cargado');
        return;
    }

    // Crear contenedores para los gráficos si no existen
    const statsContainer = document.querySelector('.stats-grid');
    if (!statsContainer) return;

    const chartsContainer = document.createElement('div');
    chartsContainer.className = 'charts-container';
    chartsContainer.innerHTML = `
        <div class="chart-card">
            <h3>Ventas por Método de Pago</h3>
            <canvas id="metodosPagoChart"></canvas>
        </div>
        <div class="chart-card">
            <h3>Estado de Créditos</h3>
            <canvas id="creditosChart"></canvas>
        </div>
    `;

    statsContainer.insertAdjacentElement('afterend', chartsContainer);
}

// Agregar estilos para los gráficos
const chartStyles = document.createElement('style');
chartStyles.textContent = `
    .charts-container {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
        gap: 20px;
        margin: 20px 0;
    }

    .chart-card {
        background: white;
        border-radius: 8px;
        padding: 20px;
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .chart-card h3 {
        margin-top: 0;
        margin-bottom: 15px;
        color: #333;
        font-size: 1.2em;
    }

    canvas {
        width: 100% !important;
        height: 300px !important;
    }
`;
document.head.appendChild(chartStyles);

// Agregar estilos para el overlay de carga
const loadingStyles = document.createElement('style');
loadingStyles.textContent = `
    .loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(255, 255, 255, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 999;
    }

    .spinner {
        border: 4px solid #f3f3f3;
        border-top: 4px solid #3498db;
        border-radius: 50%;
        width: 40px;
        height: 40px;
        animation: spin 1s linear infinite;
    }

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;
document.head.appendChild(loadingStyles);

// Agregar estilos para los controles de cantidad
const quantityStyles = document.createElement('style');
quantityStyles.textContent = `
    .quantity-control {
        display: flex;
        align-items: center;
        gap: 5px;
    }

    .quantity-control button {
        width: 25px;
        height: 25px;
        border: 1px solid #ddd;
        background: #f8f9fa;
        border-radius: 4px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        padding: 0;
    }

    .quantity-control button:hover {
        background: #e9ecef;
    }

    .quantity-control input {
        width: 50px;
        text-align: center;
        border: 1px solid #ddd;
        border-radius: 4px;
        padding: 2px;
    }

    .quantity-control input::-webkit-inner-spin-button,
    .quantity-control input::-webkit-outer-spin-button {
        -webkit-appearance: none;
        margin: 0;
    }
`;
document.head.appendChild(quantityStyles);

// Nuevos estilos para los colores de stock
const stockStyles = document.createElement('style');
stockStyles.textContent = `
    .stock-gray { background: #f3f3f3 !important; color: #888 !important; }
    .stock-amber { background: #fff8e1 !important; color: #b08500 !important; }
    .stock-blue { background: #e3f2fd !important; color: #1565c0 !important; }
`;
document.head.appendChild(stockStyles);

// Funciones para manejo de clientes
function toggleClienteSelector() {
    const tipoVenta = document.getElementById('tipoVenta').value;
    const clienteSelector = document.getElementById('clienteSelector');
    const buscarClienteInput = document.getElementById('buscarCliente');
    const clientesList = document.getElementById('clientesList');
    
    if (tipoVenta === 'CREDITO') {
        clienteSelector.style.display = 'block';
        buscarClienteInput.focus();
    } else {
        clienteSelector.style.display = 'none';
        document.getElementById('clienteId').value = '';
        buscarClienteInput.value = '';
        clientesList.innerHTML = '';
    }
}

function buscarClientes() {
    const input = document.getElementById('buscarCliente');
    const termino = input.value.trim();
    const clientesList = document.getElementById('clientesList');
    
    if (!termino) {
        clientesList.innerHTML = '';
        return;
    }

    // Mostrar indicador de carga
    clientesList.innerHTML = '<li class="loading">Buscando clientes...</li>';

    fetch(`/clientes/buscar?termino=${encodeURIComponent(termino)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la búsqueda de clientes');
            }
            return response.json();
        })
        .then(clientes => {
            clientesList.innerHTML = '';
            
            if (clientes.length === 0) {
                clientesList.innerHTML = '<li class="no-results">No se encontraron clientes</li>';
                return;
            }

            clientes.forEach(cliente => {
                const li = document.createElement('li');
                li.className = 'cliente-item';
                li.innerHTML = `
                    <div class="cliente-info">
                        <strong>${cliente.nombre} ${cliente.apellido}</strong>
                        <small>Cédula: ${cliente.cedula}</small>
                        <small>Teléfono: ${cliente.telefono || 'No disponible'}</small>
                    </div>
                `;
                li.onclick = () => seleccionarCliente(cliente);
                clientesList.appendChild(li);
            });
        })
        .catch(error => {
            console.error('Error:', error);
            clientesList.innerHTML = '<li class="error">Error al buscar clientes</li>';
            showNotification('Error al buscar clientes', 'error');
        });
}

function seleccionarCliente(cliente) {
    document.getElementById('clienteId').value = cliente.id;
    document.getElementById('buscarCliente').value = `${cliente.nombre} ${cliente.apellido} - ${cliente.cedula}`;
    document.getElementById('clientesList').innerHTML = '';
    showNotification('Cliente seleccionado: ' + cliente.nombre + ' ' + cliente.apellido, 'success');
}

// Agregar estilos para el selector de clientes
const clienteStyles = document.createElement('style');
clienteStyles.textContent = `
    .cliente-selector {
        margin: 15px 0;
        padding: 15px;
        background: #f8f9fa;
        border-radius: 8px;
        border: 1px solid #dee2e6;
    }

    .cliente-selector .form-group {
        margin-bottom: 10px;
    }

    .cliente-selector input {
        width: 100%;
        padding: 8px;
        border: 1px solid #ced4da;
        border-radius: 4px;
    }

    #clientesList {
        max-height: 200px;
        overflow-y: auto;
        margin-top: 5px;
        border: 1px solid #dee2e6;
        border-radius: 4px;
        background: white;
    }

    .cliente-item {
        padding: 10px;
        cursor: pointer;
        border-bottom: 1px solid #dee2e6;
    }

    .cliente-item:last-child {
        border-bottom: none;
    }

    .cliente-item:hover {
        background: #f8f9fa;
    }

    .cliente-info {
        display: flex;
        flex-direction: column;
        gap: 4px;
    }

    .cliente-info small {
        color: #6c757d;
    }

    .loading {
        color: #6c757d;
        text-align: center;
        padding: 10px;
    }

    .error {
        color: #dc3545;
        text-align: center;
        padding: 10px;
    }

    .no-results {
        color: #6c757d;
        text-align: center;
        padding: 10px;
    }
`;
document.head.appendChild(clienteStyles);

// Función para manejar la búsqueda de productos
function buscarProductos(query) {
    if (query.length < 2) {
        searchResults = [];
        mostrarResultadosBusqueda([]);
        return;
    }

    fetch(`/ventas/buscar?termino=${encodeURIComponent(query)}`)
        .then(response => response.json())
        .then(data => {
            searchResults = data;
            mostrarResultadosBusqueda(data);
            selectedProductIndex = -1;
        })
        .catch(error => console.error('Error:', error));
}

function mostrarResultadosBusqueda(resultados) {
    const sugerenciasList = document.getElementById('sugerenciasList');
    sugerenciasList.innerHTML = '';
    
    resultados.forEach((producto, index) => {
        const li = document.createElement('li');
        li.textContent = `${producto.nombre} - $${producto.precioVenta}`;
        li.dataset.index = index;
        li.onclick = () => seleccionarProducto(producto);
        sugerenciasList.appendChild(li);
    });
}

function seleccionarProducto(producto) {
    console.log('Seleccionando producto:', producto);
    
    // Verificar si el producto ya está en el carrito
    const index = productosSeleccionados.findIndex(p => p.id === producto.id);
    
    if (index >= 0) {
        // Si ya existe, incrementar la cantidad solo si hay stock disponible
        const productoActual = productosDisponibles.find(p => p.id === producto.id);
        if (productoActual && productosSeleccionados[index].cantidad < productoActual.cantidad) {
            productosSeleccionados[index].cantidad++;
        } else {
            showNotification('No hay más unidades disponibles', 'warning');
            return;
        }
    } else {
        // Si no existe, agregarlo con cantidad 1
        productosSeleccionados.push({
            ...producto,
            cantidad: 1
        });
    }
    
    // Verificar si podemos calcular el precio en Bs con la tasa correcta
    const tasaProducto = tasas[producto.tipoTasa || 'BCV'];
    if (!tasaProducto) {
        console.error(`No se pudo encontrar la tasa ${producto.tipoTasa} para el producto`);
        // Intentar recuperar las tasas nuevamente
        cargarTasasConReintentos(3, () => {
            actualizarTablaVentas();
        });
    } else {
        actualizarTablaVentas();
    }

    ocultarSugerencias();
    document.getElementById('buscarField').value = '';
    showNotification('Producto agregado al carrito', 'success');
}

// Función para cargar tasas con reintentos
function cargarTasasConReintentos(intentosMaximos, callback) {
    let intentos = 0;

    function intentarCargar() {
        intentos++;
        console.log(`Intento ${intentos} de cargar tasas...`);

        // Intentar cargar las tasas
        cargarTasas();

        // Verificar si se cargaron correctamente
        if (tasas['BCV']) {
            console.log('Tasas cargadas correctamente');
            if (callback) callback();
            return;
        }

        // Si no se cargaron y aún hay intentos, reintentar
        if (intentos < intentosMaximos) {
            setTimeout(intentarCargar, 1000); // Esperar 1 segundo entre intentos
        } else {
            console.error(`Fallaron ${intentosMaximos} intentos de cargar tasas`);
            showNotification('Error al cargar tasas de cambio. Intente recargar la página.', 'error');

            // Establecer una tasa por defecto como último recurso
            tasas['BCV'] = 40.0; // Valor de fallback
            if (callback) callback();
        }
    }

    intentarCargar();
}

// Actualizar el indicador de navegación
document.addEventListener('DOMContentLoaded', function() {
    const buscarField = document.getElementById('buscarField');
    if (buscarField) {
        buscarField.addEventListener('input', debounce((e) => buscarProductos(e.target.value), 300));
        buscarField.addEventListener('keydown', manejarNavegacionTeclado);
    }
    
    // Agregar el indicador de navegación (invisible)
    const indicator = document.createElement('div');
    indicator.className = 'navigation-indicator';
    indicator.style.display = 'none'; // Hacer invisible
    indicator.style.pointerEvents = 'none'; // Deshabilitar interacción
    indicator.innerHTML = `
        <div class="shortcut"><kbd>F1</kbd> Facturar</div>
        <div class="shortcut"><kbd>F2</kbd> Buscar</div>
        <div class="shortcut"><kbd>F3</kbd> Tabla</div>
        <div class="shortcut"><kbd>↑</kbd><kbd>↓</kbd> Navegar</div>
        <div class="shortcut"><kbd>Enter</kbd> Seleccionar</div>
        <div class="shortcut"><kbd>Esc</kbd> Salir</div>
    `;
    document.body.appendChild(indicator);
});

// Agregar estilos para el indicador de navegación
const navigationStyles = document.createElement('style');
navigationStyles.textContent = `
    .navigation-indicator {
        position: fixed;
        bottom: 20px;
        left: 20px;
        background: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 10px 15px;
        border-radius: 8px;
        font-size: 14px;
        display: flex;
        gap: 15px;
        z-index: 1000;
        opacity: 0.9;
        transition: opacity 0.3s;
    }

    .navigation-indicator:hover {
        opacity: 1;
    }

    .shortcut {
        display: flex;
        align-items: center;
        gap: 5px;
    }

    .shortcut kbd {
        background: #444;
        padding: 2px 6px;
        border-radius: 4px;
        font-family: monospace;
        font-size: 12px;
        border: 1px solid #666;
    }
`;
document.head.appendChild(navigationStyles);