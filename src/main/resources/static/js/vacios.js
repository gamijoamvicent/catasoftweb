document.addEventListener('DOMContentLoaded', function() {
    // Elementos del DOM
    const configForm = document.getElementById('configForm');
    const prestamoForm = document.getElementById('prestamoForm');
    const devolucionForm = document.getElementById('devolucionForm');
    const montoGarantiaInput = document.getElementById('montoGarantia');
    const cantidadVaciosInput = document.getElementById('cantidadVacios');
    const montoGarantiaPrestamoInput = document.getElementById('montoGarantiaPrestamo');
    const cantidadDevolucionInput = document.getElementById('cantidadDevolucion');
    const montoDevolverInput = document.getElementById('montoDevolver');
    const totalDisponibleElement = document.getElementById('totalDisponible');
    const totalPrestadoElement = document.getElementById('totalPrestado');
    const montoGarantiaActualElement = document.getElementById('montoGarantiaActual');

    // Estado inicial
    let montoGarantia = parseFloat(montoGarantiaInput.value);
    let totalDisponible = 0;
    let totalPrestado = 0;

    // Actualizar montos de garantía
    function actualizarMontosGarantia() {
        montoGarantiaPrestamoInput.value = montoGarantia;
        montoGarantiaActualElement.textContent = `$${montoGarantia.toFixed(2)}`;
    }

    // Configuración de garantía
    configForm.addEventListener('submit', function(e) {
        e.preventDefault();
        montoGarantia = parseFloat(montoGarantiaInput.value);
        actualizarMontosGarantia();
        alert('Configuración guardada exitosamente');
    });

    // Registro de préstamo
    prestamoForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const cantidad = parseInt(cantidadVaciosInput.value);
        
        if (cantidad <= totalDisponible) {
            const vacio = {
                cantidad: cantidad,
                montoGarantia: montoGarantia
            };

            fetch('/vacios/prestar', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                },
                body: JSON.stringify(vacio)
            })
            .then(response => response.json())
            .then(data => {
                totalDisponible -= cantidad;
                totalPrestado += cantidad;
                actualizarInventario();
                alert(`Préstamo registrado: ${cantidad} vacíos`);
                prestamoForm.reset();
            })
            .catch(error => {
                alert('Error al registrar préstamo: ' + error.message);
            });
        } else {
            alert('No hay suficientes vacíos disponibles');
        }
    });

    // Registro de devolución
    devolucionForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const cantidad = parseInt(cantidadDevolucionInput.value);
        
        if (cantidad <= totalPrestado) {
            fetch(`/vacios/devolver/${cantidad}`, {
                method: 'POST',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]').content
                }
            })
            .then(response => response.json())
            .then(data => {
                totalPrestado -= cantidad;
                totalDisponible += cantidad;
                actualizarInventario();
                alert(`Devolución registrada: ${cantidad} vacíos`);
                devolucionForm.reset();
            })
            .catch(error => {
                alert('Error al registrar devolución: ' + error.message);
            });
        } else {
            alert('La cantidad a devolver excede los vacíos prestados');
        }
    });

    // Actualizar inventario en la interfaz
    function actualizarInventario() {
        totalDisponibleElement.textContent = totalDisponible;
        totalPrestadoElement.textContent = totalPrestado;
    }

    // Inicialización
    actualizarMontosGarantia();
    actualizarInventario();
});

// Función para obtener el token CSRF
function getCsrfToken() {
    const token = document.querySelector('meta[name="_csrf"]').content;
    const header = document.querySelector('meta[name="_csrf_header"]').content;
    return { token, header };
}

// Función para manejar errores de la API
function handleApiError(error) {
    console.error('Error:', error);
    if (error.response) {
        return error.response.text().then(text => {
            try {
                const data = JSON.parse(text);
                throw new Error(data.message || 'Error en la respuesta del servidor');
            } catch (e) {
                throw new Error(text || 'Error en la respuesta del servidor');
            }
        });
    }
    throw new Error(error.message || 'Error en la respuesta del servidor');
}

// Función para mostrar mensajes de error
function showError(message) {
    if (typeof Swal !== 'undefined') {
        Swal.fire('Error', message, 'error');
    } else {
        alert('Error: ' + message);
    }
}

// Función para mostrar mensajes de éxito
function showSuccess(message) {
    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: '¡Éxito!',
            text: message,
            icon: 'success',
            showConfirmButton: false,
            timer: 1500
        }).then(() => {
            window.location.reload();
        });
    } else {
        alert('Éxito: ' + message);
        window.location.reload();
    }
}

// Función para confirmar acción
function confirmAction(message) {
    if (typeof Swal !== 'undefined') {
        return Swal.fire({
            title: '¿Está seguro?',
            text: message,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí',
            cancelButtonText: 'Cancelar'
        });
    } else {
        return Promise.resolve({ isConfirmed: confirm(message) });
    }
}

// Función para agregar nuevo stock
function agregarStock() {
    try {
        const cantidad = document.getElementById('nuevoStock').value;
        
        if (!cantidad || cantidad <= 0) {
            alert('Por favor ingrese una cantidad válida de vacíos');
            return;
        }
        
        const data = {
            cantidad: parseInt(cantidad),
            stockDisponible: parseInt(cantidad),
            valorPorUnidad: 0 // Valor inicial, se actualizará al hacer préstamos
        };
        
        const { token, header } = getCsrfToken();
        
        fetch('/vacios/prestar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                alert(data.error);
            } else {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al agregar stock: ' + error.message);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error: ' + error.message);
    }
}

// Función para editar stock
function editarStock(id) {
    try {
        const nuevaCantidad = prompt('Ingrese la nueva cantidad de vacíos en bodega:');
        if (nuevaCantidad === null) return;
        
        if (!nuevaCantidad || nuevaCantidad <= 0) {
            alert('Por favor ingrese una cantidad válida');
            return;
        }
        
        const { token, header } = getCsrfToken();
        
        fetch(`/vacios/stock/${id}?cantidad=${nuevaCantidad}`, {
            method: 'POST',
            headers: {
                [header]: token
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                alert(data.error);
            } else {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al actualizar stock: ' + error.message);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error: ' + error.message);
    }
}

// Función para eliminar stock
function eliminarStock(id) {
    try {
        if (!confirm('¿Está seguro de eliminar todo el stock de vacíos?')) return;
        
        const { token, header } = getCsrfToken();
        
        fetch(`/vacios/stock/${id}?cantidad=0`, {
            method: 'POST',
            headers: {
                [header]: token
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error en la respuesta del servidor');
            }
            return response.json();
        })
        .then(data => {
            if (data.error) {
                alert(data.error);
            } else {
                location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Error al eliminar stock: ' + error.message);
        });
    } catch (error) {
        console.error('Error:', error);
        alert('Error: ' + error.message);
    }
}

// Función para devolver un vacío
function devolverVacio(id) {
    confirmAction('¿Desea registrar la devolución de este préstamo?').then((result) => {
        if (result.isConfirmed) {
            const { token, header } = getCsrfToken();
            
            fetch(`/vacios/devolver/${id}`, {
                method: 'POST',
                headers: {
                    [header]: token
                }
            })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        try {
                            const data = JSON.parse(text);
                            throw new Error(data.message || 'Error en la respuesta del servidor');
                        } catch (e) {
                            throw new Error(text || 'Error en la respuesta del servidor');
                        }
                    });
                }
                return response.json();
            })
            .then(data => {
                // Mostrar alerta de éxito
                if (typeof Swal !== 'undefined') {
                    Swal.fire({
                        title: '¡Éxito!',
                        text: 'Préstamo devuelto exitosamente',
                        icon: 'success',
                        timer: 2000,
                        showConfirmButton: false
                    }).then(() => {
                        window.location.reload();
                    });
                } else {
                    alert('Préstamo devuelto exitosamente');
                    window.location.reload();
                }
            })
            .catch(error => {
                showError(error.message);
            });
        }
    });
}

// Manejar el formulario de préstamo
const prestamoForm = document.getElementById('prestamoForm');
if (prestamoForm) {
    prestamoForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        try {
            const cantidadInput = document.getElementById('cantidadVacios');
            const valorInput = document.getElementById('valorPorUnidad');
            
            if (!cantidadInput || !valorInput) {
                showError('No se encontraron los campos del formulario');
                return;
            }
            
            const cantidad = cantidadInput.value;
            const valor = valorInput.value;
            
            if (!cantidad || cantidad <= 0) {
                showError('Por favor ingrese una cantidad válida');
                return;
            }
            
            if (!valor || valor <= 0) {
                showError('Por favor ingrese un valor válido');
                return;
            }
            
            const data = {
                cantidad: parseInt(cantidad),
                valorPorUnidad: parseFloat(valor),
                fechaPrestamo: new Date().toISOString(),
                devuelto: false,
                esStock: false
            };
            
            const { token, header } = getCsrfToken();
            
            fetch('/vacios/prestar', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [header]: token
                },
                body: JSON.stringify(data)
            })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        try {
                            const data = JSON.parse(text);
                            throw new Error(data.message || 'Error en la respuesta del servidor');
                        } catch (e) {
                            throw new Error(text || 'Error en la respuesta del servidor');
                        }
                    });
                }
                return response.json();
            })
            .then(data => {
                showSuccess('Préstamo registrado correctamente');
            })
            .catch(error => {
                showError(error.message);
            });
        } catch (error) {
            showError(error.message);
        }
    });
}

// Manejar el formulario de devolución
const devolucionForm = document.getElementById('devolucionForm');
if (devolucionForm) {
    devolucionForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        try {
            const idInput = document.getElementById('idPrestamo');
            
            if (!idInput) {
                showError('No se encontró el campo de ID del préstamo');
                return;
            }
            
            const id = idInput.value;
            
            if (!id) {
                showError('Por favor ingrese el ID del préstamo');
                return;
            }
            
            devolverVacio(id);
        } catch (error) {
            showError(error.message);
        }
    });
}

// Event listener para el formulario de actualización de stock
const actualizarStockForm = document.getElementById('actualizarStockForm');
if (actualizarStockForm) {
    actualizarStockForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const cantidad = document.getElementById('nuevoStock').value;
        
        if (!cantidad || cantidad < 0) {
            showError('Por favor ingrese una cantidad válida');
            return;
        }

        // Obtener el ID del stock actual del elemento oculto
        const stockId = document.querySelector('input[name="stockId"]').value;

        const { token, header } = getCsrfToken();

        fetch(`/vacios/stock/${stockId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify({ cantidad: parseInt(cantidad) })
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const data = JSON.parse(text);
                        throw new Error(data.message || 'Error en la respuesta del servidor');
                    } catch (e) {
                        throw new Error(text || 'Error en la respuesta del servidor');
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            showSuccess('Stock modificado correctamente');
            location.reload();
        })
        .catch(error => {
            showError(error.message);
        });
    });
}

// Event listener para el formulario de stock inicial
const stockInicialForm = document.getElementById('stockInicialForm');
if (stockInicialForm) {
    stockInicialForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const cantidad = document.getElementById('cantidadInicial').value;
        
        if (!cantidad || cantidad <= 0) {
            showError('Por favor ingrese una cantidad válida');
            return;
        }

        const data = {
            cantidad: parseInt(cantidad),
            stockDisponible: parseInt(cantidad),
            esStock: true,
            valorPorUnidad: 0,
            fechaPrestamo: null
        };

        const { token, header } = getCsrfToken();

        fetch('/vacios/prestar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: token
            },
            body: JSON.stringify(data)
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    try {
                        const data = JSON.parse(text);
                        throw new Error(data.message || 'Error en la respuesta del servidor');
                    } catch (e) {
                        throw new Error(text || 'Error en la respuesta del servidor');
                    }
                });
            }
            return response.json();
        })
        .then(data => {
            showSuccess('Stock inicial registrado correctamente');
            location.reload();
        })
        .catch(error => {
            showError(error.message);
        });
    });
} 