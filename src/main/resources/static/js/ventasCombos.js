$(document).ready(function() {
    let carrito = [];
    let tasasDolar = {
        BCV: 0,
        PARALELA: 0,
        PROMEDIO: 0
    };
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    // Obtener las tasas del dólar
    function obtenerTasasDolar() {
        $.ajax({
            url: '/dolar/api/tasas',
            method: 'GET',
            success: function(response) {
                console.log('Respuesta tasas:', response); // Debug
                tasasDolar = {
                    BCV: parseFloat(response.bcv) || 0,
                    PARALELA: parseFloat(response.paralelo) || 0,
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
        const tipoTasa = $(this).data('tipo-tasa') || 'PROMEDIO';
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
            
            // Calcular el subtotal en Bs según el tipo de tasa del combo
            const tasa = tasasDolar[item.tipoTasa] || 0;
            console.log('Calculando subtotal para:', item.nombre, 'Tasa:', item.tipoTasa, 'Valor:', tasa); // Debug
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

        const combosVendidos = carrito.map(item => ({
            id: item.id,
            cantidad: item.cantidad
        }));

        console.log('Enviando datos de venta:', combosVendidos);

        $.ajax({
            url: '/ventas/combos/confirmar',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ items: combosVendidos }),
            success: function(response) {
                console.log('Respuesta del servidor:', response);
                if (response.success) {
                    mostrarNotificacion('✅ Venta realizada exitosamente', 'success');
                    carrito = [];
                    actualizarCarrito();
                    setTimeout(() => {
                        window.location.reload();
                    }, 2000);
                } else {
                    mostrarNotificacion('❌ ' + (response.message || 'Error al procesar la venta'), 'error');
                }
            },
            error: function(xhr, status, error) {
                console.error('Error en la petición:', { xhr, status, error });
                let mensaje = 'Error al procesar la venta';
                
                try {
                    if (xhr.responseJSON && xhr.responseJSON.message) {
                        mensaje = xhr.responseJSON.message;
                    } else if (xhr.responseText) {
                        const response = JSON.parse(xhr.responseText);
                        if (response.message) {
                            mensaje = response.message;
                        }
                    }
                } catch (e) {
                    console.error('Error al procesar la respuesta:', e);
                    if (xhr.responseText) {
                        mensaje = xhr.responseText;
                    }
                }
                
                mostrarNotificacion('❌ ' + mensaje, 'error');
            }
        });
    });
}); 