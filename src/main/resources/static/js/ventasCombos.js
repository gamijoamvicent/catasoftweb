$(document).ready(function() {
    let carrito = [];
    const token = $("meta[name='_csrf']").attr("content");
    const header = $("meta[name='_csrf_header']").attr("content");

    // Configurar CSRF para todas las peticiones AJAX
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader(header, token);
        }
    });

    // Agregar combo al carrito
    $('.agregar-combo').click(function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const precio = parseFloat($(this).data('precio'));

        // Verificar si el combo ya está en el carrito
        const comboExistente = carrito.find(item => item.id === id);
        if (comboExistente) {
            comboExistente.cantidad++;
        } else {
            carrito.push({
                id: id,
                nombre: nombre,
                precio: precio,
                cantidad: 1
            });
        }

        actualizarCarrito();
        mostrarNotificacion('Combo agregado al carrito');
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
        mostrarNotificacion('Combo eliminado del carrito');
    });

    // Actualizar la vista del carrito
    function actualizarCarrito() {
        const $cartItems = $('.cart-items');
        $cartItems.empty();

        let total = 0;

        carrito.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            total += subtotal;

            const $item = $(`
                <div class="cart-item">
                    <div class="cart-item-info">
                        <div class="cart-item-title">${item.nombre}</div>
                        <div class="cart-item-price">$${item.precio.toFixed(2)} c/u</div>
                    </div>
                    <div class="cart-item-quantity">
                        <button class="quantity-btn" data-id="${item.id}" data-accion="decrementar">-</button>
                        <span>${item.cantidad}</span>
                        <button class="quantity-btn" data-id="${item.id}" data-accion="incrementar">+</button>
                    </div>
                    <div class="cart-item-subtotal">
                        $${subtotal.toFixed(2)}
                    </div>
                    <button class="remove-item" data-id="${item.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            `);

            $cartItems.append($item);
        });

        $('.total-amount').text(`$${total.toFixed(2)}`);
        $('#btnConfirmarVenta').prop('disabled', carrito.length === 0);
    }

    // Confirmar venta
    $('#btnConfirmarVenta').click(function() {
        if (carrito.length === 0) {
            mostrarNotificacion('El carrito está vacío', 'error');
            return;
        }

        const ventaData = {
            items: carrito.map(item => ({
                id: item.id,
                cantidad: item.cantidad
            }))
        };

        $.ajax({
            url: '/ventas/combos/confirmar',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(ventaData),
            success: function(response) {
                if (response.success) {
                    mostrarNotificacion('Venta realizada con éxito', 'success');
                    carrito = [];
                    actualizarCarrito();
                    
                    // Redirigir al ticket si existe
                    if (response.ticketUrl) {
                        window.location.href = response.ticketUrl;
                    }
                } else {
                    mostrarNotificacion(response.message, 'error');
                }
            },
            error: function(xhr) {
                mostrarNotificacion('Error al procesar la venta: ' + xhr.responseText, 'error');
            }
        });
    });

    // Función para mostrar notificaciones
    function mostrarNotificacion(mensaje, tipo = 'info') {
        const $notificacion = $(`
            <div class="notificacion ${tipo}">
                ${mensaje}
            </div>
        `);

        $('body').append($notificacion);

        setTimeout(() => {
            $notificacion.fadeOut(() => {
                $notificacion.remove();
            });
        }, 3000);
    }
}); 