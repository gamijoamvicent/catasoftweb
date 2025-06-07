// Lógica de búsqueda y sugerencias para productos y combos

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
        li.setAttribute('data-tipo', prod.tipo || 'producto');
        let label = '';
        if (prod.tipo === 'combo') {
            li.className = 'producto-item stock-blue';
            label = `<strong>Combo: ${prod.nombre}</strong><br><small>Precio: $${prod.precioVenta}</small>`;
        } else {
            let stockClass = '';
            if (prod.cantidad === 0) {
                stockClass = 'stock-gray';
            } else if (prod.cantidad <= 10) {
                stockClass = 'stock-amber';
            } else {
                stockClass = 'stock-blue';
            }
            li.className = `producto-item ${stockClass}`;
            label = `<strong>${prod.nombre}</strong><br><small>Precio: $${prod.precioVenta.toFixed(2)} | Stock: ${prod.cantidad}</small>`;
        }
        li.innerHTML = label;
        li.style.cursor = 'pointer';
        li.addEventListener('click', handleProductClick);
        lista.appendChild(li);
    });
}

function handleProductClick(event) {
    event.preventDefault();
    event.stopPropagation();
    const li = event.currentTarget;
    const productoId = li.getAttribute('data-id');
    const tipo = li.getAttribute('data-tipo');
    if (tipo === 'combo') {
        fetch(`/combos/api/combos/${productoId}/detalle`)
            .then(res => res.json())
            .then(combo => {
                if (!combo || !combo.id) return;
                agregarAlCarrito({
                    id: combo.id,
                    nombre: `Combo: ${combo.nombre}`,
                    precioVenta: parseFloat(combo.precio),
                    cantidad: 1,
                    tipo: 'combo',
                    productos: combo.productos
                });
                document.getElementById('buscarField').value = '';
                document.getElementById('buscarField').focus();
            })
            .catch(err => {
                if (typeof showNotification === 'function') {
                    showNotification('Error al agregar combo: ' + err.message, 'error');
                } else {
                    alert('Error al agregar combo: ' + err.message);
                }
            });
    } else {
        if (typeof productosDisponibles !== 'undefined') {
            const producto = productosDisponibles.find(p => p.id === parseInt(productoId));
            if (producto && producto.cantidad > 0) {
                agregarAlCarrito(producto);
                document.getElementById('buscarField').value = '';
                document.getElementById('buscarField').focus();
            }
        }
    }
}
