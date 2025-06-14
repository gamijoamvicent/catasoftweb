// reportesCombos.js

let allCombosData = [];

function renderCombosTable(data) {
    // Limpiar tabla
    const tabla = document.getElementById('tabla-combos');
    tabla.innerHTML = '';
    // KPIs
    let totalCombos = 0;
    let ingresosCombos = 0;
    let ticketPromedio = 0;
    let comboCount = {};
    let topCombo = '-';
    data.forEach(vc => {
        totalCombos += 1;
        ingresosCombos += Number(vc.valorVentaUSD || 0);
        let nombreCombo = vc.comboNombre || 'N/A';
        comboCount[nombreCombo] = (comboCount[nombreCombo] || 0) + 1;
        // Tabla
        let fecha = (vc.fechaVenta || '').substring(0, 10);
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${fecha}</td><td>${nombreCombo}</td><td>1</td><td>${vc.clienteNombre || '-'}</td><td>${vc.metodoPago || '-'}</td><td>$${vc.valorVentaUSD || 0}</td><td>${vc.valorVentaBS || 0} Bs</td>`;
        tabla.appendChild(tr);
    });
    document.getElementById('total-combos').textContent = totalCombos;
    document.getElementById('ingresos-combos').textContent = `$${ingresosCombos.toFixed(2)}`;
    ticketPromedio = totalCombos > 0 ? ingresosCombos / totalCombos : 0;
    document.getElementById('ticket-promedio-combo').textContent = `$${ticketPromedio.toFixed(2)}`;
    let top = Object.entries(comboCount).sort((a,b)=>b[1]-a[1])[0];
    if(top) document.getElementById('combo-top').textContent = top[0];
}

document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/reportes/combos')
        .then(res => res.json())
        .then(data => {
            allCombosData = data;
            renderCombosTable(data);
        });

    document.getElementById('filtrar-combos').addEventListener('click', function() {
        const inicio = document.getElementById('fecha-inicio').value;
        const fin = document.getElementById('fecha-fin').value;
        let filtrados = allCombosData;
        if (inicio) {
            filtrados = filtrados.filter(vc => (vc.fechaVenta || '').substring(0,10) >= inicio);
        }
        if (fin) {
            filtrados = filtrados.filter(vc => (vc.fechaVenta || '').substring(0,10) <= fin);
        }
        renderCombosTable(filtrados);
    });

    // Botón para exportar PDF
    const exportBtn = document.createElement('button');
    exportBtn.className = 'btn btn-danger mb-3';
    exportBtn.innerHTML = '<i class="fas fa-file-pdf"></i> Descargar PDF';
    exportBtn.onclick = function() {
        const inicio = document.getElementById('fecha-inicio').value;
        const fin = document.getElementById('fecha-fin').value;
        let url = '/api/reportes/combos/pdf';
        if (inicio || fin) {
            url += `?inicio=${inicio || ''}&fin=${fin || ''}`;
        }
        window.open(url, '_blank');
    };
    document.querySelector('.dashboard-header').appendChild(exportBtn);
});
