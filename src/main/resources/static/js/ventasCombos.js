$(document).ready(function() {
    // Variables globales
    let carrito = [];
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");
    const precioDolar = parseFloat(document.getElementById("precioDolarLabel")?.textContent.replace(' Bs', '') || 1);

    // Obtener las tasas del dólar
    function obtenerTasasDolar() {
        $.ajax({
            url: '/dolar/api/tasas',
            method: 'GET',
            success: function(response) {
                console.log('Respuesta tasas:', response); // Debug
                console.log('Tasa paralela en respuesta:', response.paralela || response.paralelo); // Debug específico para tasa paralela

                // Asegurar que tenemos un valor para la tasa paralela
                const tasaParalela = parseFloat(response.paralela) || parseFloat(response.paralelo) || 0;
                console.log('Tasa PARALELA procesada:', tasaParalela);

                tasasDolar = {
                    BCV: parseFloat(response.bcv) || 0,
                    PARALELA: tasaParalela,
                    PROMEDIO: parseFloat(response.promedio) || 0
                };
                console.log('Tasas procesadas:', tasasDolar); // Debug
                actualizarCarrito(); // Actualizar los totales con las nuevas tasas
            },
            error: function(xhr) {
                const mensaje = xhr.responseText || 'Error al obtener las tasas del dólar';
                mostrarNotificacion(mensaje, 'error');
                // Intentar nuevamente en 5 segundos
                setTimeout(obtenerTasasDolar, 5000);
            }
        });
    }

    // Obtener las tasas iniciales
    obtenerTasasDolar();

    // Configurar CSRF para todas las peticiones AJAX
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        }
    });

    // Función de búsqueda
    $('#searchInput').on('input', function() {
        const searchTerm = $(this).val().toLowerCase();
        $('.combo-card').each(function() {
            const comboNombre = $(this).data('nombre').toLowerCase();
            if (comboNombre.includes(searchTerm)) {
                $(this).show();
            } else {
                $(this).hide();
            }
        });
    });

    // Función para mostrar notificaciones
    function mostrarNotificacion(mensaje, tipo = 'success') {
        console.log('Mostrando notificación:', mensaje, tipo);
        
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000,
            timerProgressBar: true,
            didOpen: (toast) => {
                toast.addEventListener('mouseenter', Swal.stopTimer)
                toast.addEventListener('mouseleave', Swal.resumeTimer)
            }
        });

        let icon = 'success';
        switch(tipo) {
            case 'error':
                icon = 'error';
                break;
            case 'warning':
                icon = 'warning';
                break;
            case 'info':
                icon = 'info';
                break;
        }

        Toast.fire({
            icon: icon,
            title: mensaje
        });
    }

    // Agregar combo al carrito
    $('.agregar-combo').click(function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const precio = parseFloat($(this).data('precio'));
        let tipoTasa = $(this).data('tipo-tasa') || 'PROMEDIO';

        // Normalizar el tipo de tasa al agregar al carrito
        if (tipoTasa === 'PARALELO') {
            console.log('Normalizando tipo de tasa PARALELO a PARALELA');
            tipoTasa = 'PARALELA';
        }

        console.log('Agregando combo:', { id, nombre, precio, tipoTasa });

        const comboExistente = carrito.find(item => item.id === id);
        if (comboExistente) {
            comboExistente.cantidad++;
        } else {
            carrito.push({
                id: id,
                nombre: nombre,
                precio: precio,
                tipoTasa: tipoTasa,
                cantidad: 1
            });
        }

        actualizarCarrito();
        mostrarNotificacion('Combo agregado al carrito', 'success');
    });

    // Actualizar cantidad de un combo en el carrito
    $(document).on('click', '.quantity-btn', function() {
        const id = $(this).data('id');
        const accion = $(this).data('accion');
        const combo = carrito.find(item => item.id === id);

        if (combo) {
            if (accion === 'incrementar') {
                combo.cantidad++;
            } else if (accion === 'decrementar' && combo.cantidad > 1) {
                combo.cantidad--;
            }
            actualizarCarrito();
        }
    });

    // Eliminar combo del carrito
    $(document).on('click', '.remove-item', function() {
        const id = $(this).data('id');
        carrito = carrito.filter(item => item.id !== id);
        actualizarCarrito();
        mostrarNotificacion('Combo eliminado del carrito', 'warning');
    });

    // Actualizar la vista del carrito
    function actualizarCarrito() {
        const $cartItems = $('.cart-items');
        $cartItems.empty();

        let total = 0;
        let totalBs = 0;

        carrito.forEach((item, index) => {
            const subtotal = item.precio * item.cantidad;
            total += subtotal;
            
            // Normalizar el tipo de tasa (convertir PARALELO a PARALELA si es necesario)
            let tipoTasaNormalizado = item.tipoTasa;
            if (tipoTasaNormalizado === 'PARALELO') {
                tipoTasaNormalizado = 'PARALELA';
            }

            // Calcular el subtotal en Bs según el tipo de tasa del combo
            const tasa = tasasDolar[tipoTasaNormalizado] || 0;
            console.log('Calculando subtotal para:', item.nombre, 'Tasa Original:', item.tipoTasa, 'Tasa Normalizada:', tipoTasaNormalizado, 'Valor:', tasa); // Debug
            const subtotalBs = subtotal * tasa;
            totalBs += subtotalBs;

            $cartItems.append(`
                <div class="cart-item">
                    <div class="item-info">
                        <span class="item-name">${item.nombre}</span>
                        <span class="item-price">$${item.precio.toFixed(2)}</span>
                    </div>
                    <div class="item-quantity">
                        <button class="quantity-btn" data-id="${item.id}" data-accion="decrementar">-</button>
                        <span>${item.cantidad}</span>
                        <button class="quantity-btn" data-id="${item.id}" data-accion="incrementar">+</button>
                    </div>
                    <div class="item-subtotal">
                        <span>$${subtotal.toFixed(2)}</span>
                        <button class="remove-item" data-id="${item.id}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            `);
        });

        // Actualizar totales
        $('.total-amount').html(`
            <div class="total-usd">$${total.toFixed(2)}</div>
            <div class="total-bs">Bs. ${totalBs.toFixed(2)}</div>
            <div class="tasa-info">
                BCV: ${tasasDolar.BCV?.toFixed(2) || 'N/A'} Bs. | 
                Paralela: ${tasasDolar.PARALELA?.toFixed(2) || 'N/A'} Bs. | 
                Promedio: ${tasasDolar.PROMEDIO?.toFixed(2) || 'N/A'} Bs.
            </div>
        `);
        $('#btnConfirmarVenta').prop('disabled', carrito.length === 0);
    }

    // Función para confirmar la venta
    $('#btnConfirmarVenta').on('click', function() {
        console.log('Botón de confirmar venta clickeado');
        if (carrito.length === 0) {
            mostrarNotificacion('El carrito está vacío', 'warning');
            return;
        }

        // Obtener el token CSRF
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

        // Preparar el array de ventas
        const ventasData = carrito.map(combo => {
            const valorUSD = combo.precio;
            let tipoTasaNormalizado = combo.tipoTasa;
            if (tipoTasaNormalizado === 'PARALELO') {
                tipoTasaNormalizado = 'PARALELA';
            }
            const tasaEfectiva = tasasDolar[tipoTasaNormalizado] || tasasDolar['BCV'];
            const valorBS = valorUSD * tasaEfectiva;
            return {
                comboId: combo.id,
                valorVentaUSD: valorUSD,
                valorVentaBS: valorBS,
                tasaConversion: tasaEfectiva,
                metodoPago: 'EFECTIVO',
                cantidad: combo.cantidad
            };
        });

        fetch('/ventas/combos/registrar-multiple', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify(ventasData)
        })
        .then(response => response.json())
        .then(result => {
            if (result.mensaje && result.mensaje.includes('exitosamente')) {
                mostrarNotificacion('✅ Venta(s) registrada(s) exitosamente', 'success');
                carrito = [];
                actualizarCarrito();
            } else {
                throw new Error(result.error || 'Error desconocido');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarNotificacion('❌ Error al registrar la venta: ' + error.message, 'error');
        });
    });

    // Función para cargar los combos
    function cargarCombos() {
        $.get('/combos/api/combos', function(combos) {
            const $listaCombos = $('#listaCombos');
            $listaCombos.empty();
            
            combos.forEach(combo => {
                // Obtener detalles del combo
                $.get(`/combos/api/combos/${combo.id}/detalle`, function(detalle) {
                    const requiereActualizacion = detalle.requiereActualizacion;
                    const $combo = $(`
                        <div class="col-md-6 mb-4">
                            <div class="card ${requiereActualizacion ? 'border-warning' : ''}">
                                <div class="card-body">
                                    <div class="d-flex justify-content-between align-items-center mb-3">
                                        <h5 class="card-title">
                                            ${combo.nombre}
                                            ${requiereActualizacion ? 
                                                '<span class="badge bg-warning text-dark ms-2">Requiere actualización</span>' : 
                                                '<span class="badge bg-success ms-2">Listo para vender</span>'}
                                        </h5>
                                    </div>
                                    <p class="precio-combo">$${combo.precio}</p>
                                    <div class="productos-list mb-3">
                                        <div id="productos-${combo.id}">
                                            <!-- Los productos se cargarán dinámicamente -->
                                        </div>
                                    </div>
                                    ${requiereActualizacion ? 
                                        `<div class="alert alert-warning">
                                            <small>${detalle.mensajeActualizacion}</small>
                                        </div>
                                        <button class="btn btn-warning btn-sm" onclick="actualizarCombo(${combo.id})">
                                            <i class="fas fa-sync-alt me-1"></i>
                                            Actualizar Combo
                                        </button>` : 
                                        `<button class="btn btn-primary btn-sm agregar-combo" 
                                            data-id="${combo.id}" 
                                            data-nombre="${combo.nombre}" 
                                            data-precio="${combo.precio}"
                                            data-tipo-tasa="${combo.tipoTasa}">
                                            <i class="fas fa-cart-plus me-1"></i>
                                            Agregar al Carrito
                                        </button>`
                                    }
                                </div>
                            </div>
                        </div>
                    `);
                    
                    $listaCombos.append($combo);
                    
                    // Cargar productos del combo
                    const $productos = $(`#productos-${combo.id}`);
                    $productos.empty();
                    
                    if (detalle.productos.length === 0) {
                        $productos.append(`<div class="text-muted">No hay productos en este combo</div>`);
                    } else {
                        detalle.productos.forEach(producto => {
                            $productos.append(`
                                <div class="d-flex justify-content-between align-items-center mb-2">
                                    <span class="fw-medium ${!producto.activo ? 'text-danger' : ''}">
                                        ${producto.nombre}
                                        ${!producto.activo ? 
                                            '<i class="fas fa-exclamation-circle ms-1" title="Producto inactivo"></i>' : ''}
                                    </span>
                                    <span class="badge bg-light text-dark">x${producto.cantidad}</span>
                                </div>
                            `);
                        });
                    }
                });
            });
        });
    }

    // Función para actualizar un combo
    function actualizarCombo(comboId) {
        Swal.fire({
            title: 'Actualizar Combo',
            text: '¿Desea actualizar este combo? Será redirigido a la página de edición.',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Sí, actualizar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = `/combos/editar/${comboId}`;
            }
        });
    }
});