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