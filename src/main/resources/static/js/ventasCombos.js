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

    // Agregar combo al carrito
    $('.agregar-combo').click(function() {
        const id = $(this).data('id');
        const nombre = $(this).data('nombre');
        const precio = parseFloat($(this).data('precio'));
        const tipoTasa = $(this).data('tipo-tasa') || 'PROMEDIO'; // Valor por defecto
        console.log('Agregando combo:', { id, nombre, precio, tipoTasa }); // Debug

        // Verificar si el combo ya está en el carrito
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
        let totalBs = 0;

        carrito.forEach(item => {
            const subtotal = item.precio * item.cantidad;
            total += subtotal;
            
            // Calcular el subtotal en Bs según el tipo de tasa del combo
            const tasa = tasasDolar[item.tipoTasa] || 0;
            console.log('Calculando subtotal para:', item.nombre, 'Tasa:', item.tipoTasa, 'Valor:', tasa); // Debug
            const subtotalBs = subtotal * tasa;
            totalBs += subtotalBs;

            const $item = $(`
                <div class="cart-item" data-id="${item.id}">
                    <div class="cart-item-info">
                        <div class="cart-item-title">${item.nombre}</div>
                        <div class="cart-item-price">$${item.precio.toFixed(2)} c/u (${item.tipoTasa})</div>
                    </div>
                    <div class="cart-item-quantity">
                        <button class="quantity-btn" data-id="${item.id}" data-accion="decrementar">-</button>
                        <span class="quantity">${item.cantidad}</span>
                        <button class="quantity-btn" data-id="${item.id}" data-accion="incrementar">+</button>
                    </div>
                    <div class="cart-item-subtotal">
                        $${subtotal.toFixed(2)}
                        <div class="subtotal-bs">Bs. ${subtotalBs.toFixed(2)}</div>
                    </div>
                    <button class="remove-item" data-id="${item.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            `);

            $cartItems.append($item);
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

    // Función para mostrar notificaciones
    function mostrarNotificacion(mensaje, tipo = 'success') {
        const notificacion = document.createElement('div');
        notificacion.className = `notificacion ${tipo}`;
        notificacion.textContent = mensaje;
        document.body.appendChild(notificacion);

        setTimeout(() => {
            notificacion.remove();
        }, 3000);
    }

    // Función para confirmar la venta
    function confirmarVenta() {
        if (carrito.length === 0) {
            mostrarNotificacion('El carrito está vacío', 'warning');
            return;
        }

        const combosVendidos = carrito.map(item => ({
            id: item.id,
            cantidad: item.cantidad
        }));

        $.ajax({
            url: '/combos/api/combos/venta',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ combos: combosVendidos }),
            success: function(response) {
                mostrarNotificacion('Venta realizada exitosamente');
                // Limpiar el carrito
                carrito = [];
                actualizarCarrito();
            },
            error: function(xhr) {
                const mensaje = xhr.responseText || 'Error al procesar la venta';
                mostrarNotificacion(mensaje, 'error');
            }
        });
    }

    // Agregar el evento al botón de confirmar venta
    $('#btnConfirmarVenta').click(confirmarVenta);
}); 