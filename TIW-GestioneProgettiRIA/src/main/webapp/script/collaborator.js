let loadedProjects=[];

document.addEventListener('DOMContentLoaded', () => {
	loadUserProfile();	
    loadInitialData();
    setupProjectSelectorListener();
    setupMatrixCellListener();
});

// ===========================
// CARICAMENTO DATI DAL SERVER
// ===========================
function loadInitialData() 
{
    sendAsyncRequest('GET', 'Collaborator', null, (xhr) => {
        if(xhr.readyState===4) 
		{
            if(xhr.status===200) 
			{
                try 
				{
                    const data=JSON.parse(xhr.responseText);
                    loadedProjects=data.assignedProjects || [];
                    populateProjectsDropdown(loadedProjects);
                    
                    document.body.style.visibility='visible';
                }
				catch(e) 
				{
                    console.error("Errore nel parsing dei dati collaboratore:", e);
                }
				
				document.body.style.visibility='visible';
	        } 
			else
			{
				window.location.href="login.html";
	            console.error("Impossibile caricare i dati dall'endpoint Admin.");
			}
		}
    });
}

function populateProjectsDropdown(projects) 
{
    const select=document.getElementById('idProgettoCollaboratore');
    if(!select) return;

    select.innerHTML='<option value="" disabled selected>-- Scegli un Progetto --</option>';
    if(!projects) return;

    // Ottimizzazione: DocumentFragment per ridurre le manipolazioni del DOM
    const fragment=document.createDocumentFragment();
    projects.forEach(p => {
        const opt=document.createElement('option');
        opt.value=p.id;
        opt.textContent=`${p.nomeProgetto} (Durata: ${p.durata} Mesi)`;
        fragment.appendChild(opt);
    });
	
    select.appendChild(fragment);
}

function setupProjectSelectorListener() {
    const select=document.getElementById('idProgettoCollaboratore');
    if(select) 
	{
        select.addEventListener('change', (e) => {
            const selectedId=parseInt(e.target.value, 10);
            if(!isNaN(selectedId))
                renderCollaboratorMatrix(selectedId);
        });
    }
}

// =================
// RENDERING TABELLA
// =================
function renderCollaboratorMatrix(projectId) {
    const project=loadedProjects.find(p => p.id===projectId);
    const section=document.getElementById('collaborator-matrix-section');
    const msgEmpty=document.getElementById('no-project-selected-msg');
    const table=document.getElementById('collaborator-hours-table');
	
    if(!project) 
	{
        toggleMatrixVisibility(false, section, msgEmpty);
        return;
    }
	
    toggleMatrixVisibility(true, section, msgEmpty);

    table.textContent='';

    // Costruiamo e appendiamo intestazione e corpo in modo ottimizzato
    table.appendChild(createTableHeader(project.durata));
    table.appendChild(createTableBody(project.wps, project.durata));
}

function toggleMatrixVisibility(hasProject, section, msgEmpty) 
{
    if(msgEmpty) msgEmpty.classList.toggle('hidden-section', hasProject);
    if(section) section.classList.toggle('hidden-section', !hasProject);
}

/**
 * Crea l'elemento <thead> in modo sicuro e veloce
 */
function createTableHeader(duration) 
{
    const thead=document.createElement('thead');
    const tr=document.createElement('tr');
    
    let headerRow='<th style="min-width: 150px;">Work Package</th><th style="min-width: 180px;">Task</th>';
    for(let m=1; m<=duration; m++)
        headerRow+=`<th class="td-durata">M${m}</th>`;

    headerRow+='</tr>';
    tr.innerHTML=headerRow;
    thead.appendChild(tr);
    return thead;
}

/**
 * Crea il corpo <tbody> usando DocumentFragment per massimizzare le performance
 */
function createTableBody(wps, duration) 
{
    const tbody=document.createElement('tbody');
    if(!wps) return tbody;

    const fragment=document.createDocumentFragment();

    wps.forEach(wp => {
        const tasks=wp.tasks || [];
        const totalTasks=tasks.length;
        if(totalTasks===0) return;

        tasks.forEach((task, tIdx) => {
            const tr=document.createElement('tr');
            let rowHtml='';

            // Rowspan per il nome del WP (solo sulla prima riga del gruppo)
            if(tIdx===0)
                rowHtml+=`<td rowspan="${totalTasks}" class="td-wp-name" style="font-weight: bold; background: #fafafa; vertical-align: middle;">${wp.numeroOrdine}. ${wp.titolo}</td>`;
            
            rowHtml+=`<td style="text-align: left; padding-left: 10px; font-weight: 500;">${wp.numeroOrdine}.${task.numeroOrdine} ${task.nomeTask}</td>`;
            rowHtml+=createMonthlyCellsHtml(task, duration);

            tr.innerHTML=rowHtml;
            fragment.appendChild(tr);
        });
    });

    tbody.appendChild(fragment);
    return tbody;
}

function createMonthlyCellsHtml(task, duration) 
{
    let cellsHtml='';

    for(let monthNum=1; monthNum<=duration; monthNum++) 
	{
        const currentHours=(task.oreLavorateMese && task.oreLavorateMese[monthNum]!==undefined)?task.oreLavorateMese[monthNum]:0;
        const isMonthValid=(monthNum>=task.meseInizio && monthNum<=task.meseFine);

        if(isMonthValid) 
		{
            cellsHtml+=`
                <td class="editable-cell" style="cursor: pointer;" 
                    data-task-id="${task.id}" 
                    data-month="${monthNum}" 
                    data-current-value="${currentHours}">
                    ${currentHours}
                </td>`;
        } 
		else
            cellsHtml+='<td>-</td>';
    }

    return cellsHtml;
}


// ========================
// EVENTO DI INLINE-EDITING
// ========================
function setupMatrixCellListener() 
{
    const table=document.getElementById('collaborator-hours-table');
    if(!table) return;

    // Un unico ascoltatore sulla tabella
    table.addEventListener('click', (e) => {
        const cell=e.target.closest('.editable-cell');
        
		//Se cella non editabile
        if(!cell || cell.querySelector('input')) return;

        const currentVal=cell.getAttribute('data-current-value');
        const input=document.createElement('input');
        input.type='number';
        input.min='0';
        input.value=currentVal;
        input.className='matrix-inline-input';
        input.style.width='50px';
        input.style.textAlign='center';

        cell.textContent='';
        cell.appendChild(input);
        input.focus();

        let hasSaved=false;

        const triggerAutoSave=() => {
            if(hasSaved) return;
            hasSaved=true;

            const newValueStr=input.value.trim();
            const newValue=parseInt(newValueStr, 10);
            const taskId=parseInt(cell.getAttribute('data-task-id'), 10);
            const monthNum=parseInt(cell.getAttribute('data-month'), 10);

            // Validazione client-side immediata
            if(newValueStr==="" || isNaN(newValue) || newValue<0) 
			{
                showFeedback("Errore: inserisci un numero intero maggiore o uguale a zero.", false);
                cell.textContent=currentVal;
                return;
            }

            //Valore non cambiato
            if(newValue===parseInt(currentVal, 10)) 
			{
                cell.textContent=currentVal;
                return;
            }
			
            executeAutoSaveHours(taskId, monthNum, newValue, cell, currentVal);
        };

        input.addEventListener('blur', triggerAutoSave);
        input.addEventListener('keydown', (evt) => {
            if(evt.key==='Enter') 
			{
                evt.preventDefault();
                input.blur();
            }
        });
    });
}

function executeAutoSaveHours(taskId, month, hours, cellElement, fallbackValue) 
{
    const payload={
        idTask: taskId,
        mese: month,
        oreLavorate: hours
    };

    sendAsyncRequest('POST', 'DoSaveHours', payload, (xhr) => {
        if(xhr.readyState===4) 
		{
            if(xhr.status===200) 
			{
                showFeedback("Ore aggiornate ed auto-salvate con successo.", true);
                cellElement.setAttribute('data-current-value', hours);
                cellElement.textContent=hours;
                
                // Aggiornamento ottimizzato della memoria locale (cicli interrotti subito col 'break')
                const currentProjId=parseInt(document.getElementById('idProgettoCollaboratore').value, 10);
                const project=loadedProjects.find(p => p.id===currentProjId);
                
                if(project && project.wps) 
                    for(const wp of project.wps) 
                        if(wp.tasks) 
						{
                            const t=wp.tasks.find(tk => tk.id===taskId);
                            if(t) 
							{
                                t.oreLavorateMese=t.oreLavorateMese || {};
                                t.oreLavorateMese[month]=hours;
                                break;
                            }
                        }
            } 
			else 
			{
                showFeedback(`Errore durante il salvataggio: ${xhr.responseText}`, false);
                cellElement.textContent=fallbackValue;
            }
        }
    });
}