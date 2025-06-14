// reportesCombos.js

document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/reportes/combos')
        .then(res => res.json())
        .then(data => {
            // KPIs
            let totalCombos = 0;
            let ingresosCombos = 0;
            let ticketPromedio = 0;
            let comboCount = {};
            let topCombo = '-';
            let combosPorDia = {};
            let tabla = document.getElementById('tabla-combos');

            data.forEach(vc => {
                totalCombos += 1;
                ingresosCombos += Number(vc.valorVentaUSD || 0);
                let nombreCombo = vc.comboNombre || 'N/A';
                comboCount[nombreCombo] = (comboCount[nombreCombo] || 0) + 1;
                // Agrupar por día
                let fecha = (vc.fechaVenta || '').substring(0, 10);
                combosPorDia[fecha] = (combosPorDia[fecha] || 0) + 1;
                // Tabla
                const tr = document.createElement('tr');
                tr.innerHTML = `<td>${fecha}</td><td>${nombreCombo}</td><td>1</td><td>${vc.clienteNombre || '-'}</td><td>${vc.metodoPago || '-'}</td><td>$${vc.valorVentaUSD || 0}</td><td>${vc.valorVentaBS || 0}</td>`;
                tabla.appendChild(tr);
            });
            // KPIs
            document.getElementById('total-combos').textContent = totalCombos;
            document.getElementById('ingresos-combos').textContent = `$${ingresosCombos.toFixed(2)}`;
            ticketPromedio = totalCombos > 0 ? ingresosCombos / totalCombos : 0;
            document.getElementById('ticket-promedio-combo').textContent = `$${ticketPromedio.toFixed(2)}`;
            // Top combo
            let top = Object.entries(comboCount).sort((a,b)=>b[1]-a[1])[0];
            if(top) document.getElementById('combo-top').textContent = top[0];
            // Gráficos
            new Chart(document.getElementById('combosPorDiaChart').getContext('2d'), {
                type: 'line',
                data: {
                    labels: Object.keys(combosPorDia),
                    datasets: [{
                        label: 'Combos Vendidos',
                        data: Object.values(combosPorDia),
                        backgroundColor: 'rgba(21,101,192,0.1)',
                        borderColor: '#1565c0',
                        borderWidth: 2,
                        fill: true,
                        tension: 0.3
                    }]
                },
                options: { responsive: true, plugins: { legend: { display: false } } }
            });
            // Top combos
            let topCombosArr = Object.entries(comboCount).sort((a,b)=>b[1]-a[1]).slice(0,5);
            new Chart(document.getElementById('topCombosChart').getContext('2d'), {
                type: 'bar',
                data: {
                    labels: topCombosArr.map(e=>e[0]),
                    datasets: [{
                        label: 'Ventas',
                        data: topCombosArr.map(e=>e[1]),
                        backgroundColor: '#00BFA5',
                        borderRadius: 8
                    }]
                },
                options: { responsive: true, plugins: { legend: { display: false } } }
            });
        });
});
