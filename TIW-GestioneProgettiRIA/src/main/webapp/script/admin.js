let loadedProjects=[];

document.addEventListener('DOMContentLoaded', () => {
	loadUserProfile();	
    loadInitialData();

    // Riferimenti ai blocchi di sezione
    const sectionCreation=document.getElementById('section-creation');
    const sectionMonitor=document.getElementById('section-monitor');

    // Riferimenti ai pulsanti dell'Header
    const btnToggleVerify=document.getElementById('btn-toggle-verify');
    const btnToggleCreate=document.getElementById('btn-toggle-create');
	
	// Pulsanti Undo / Redo
    const btnUndo=document.getElementById('btn-undo');
    const btnRedo=document.getElementById('btn-redo');
    if(btnUndo) btnUndo.addEventListener('click', performUndo);
    if(btnRedo) btnRedo.addEventListener('click', performRedo);

    updateUndoRedoButtons();

    // Scorciatoie da tastiera Ctrl+Z / Ctrl+Y
    document.addEventListener('keydown', (e) => {
        if(e.ctrlKey || e.metaKey)
            if(e.key==='z' || e.key==='Z') 
			{
                e.preventDefault();
                performUndo();
            } 
			else if(e.key==='y' || e.key==='Y') 
			{
                e.preventDefault();
                performRedo();
            }
    });

	// ==========================================
    // AGGANCIO TRIGGER DI UNDO/REDO SUI CAMPI (ORIGINALE)
    // ==========================================
    const form=document.getElementById('bulk-project-form');
    if(form) 
	{
		let debounceTimer;

        form.addEventListener('focusout', (e) => {
            if(e.target.tagName==='INPUT' || e.target.tagName==='TEXTAREA' || e.target.tagName==='SELECT') 
			{
				clearTimeout(debounceTimer);
                saveState();
            }
        });

        // 2. Se l'utente digita, invalida semplicemente la cronologia Redo (senza salvare nuovi stati Undo)
		form.addEventListener('input', (e) => {
            if(e.target.tagName==='INPUT' || e.target.tagName==='TEXTAREA' || e.target.tagName==='SELECT') 
			{
                if(redoStack.length>0) 
				{
                    redoStack.length=0;
                    updateUndoRedoButtons();
                }

                clearTimeout(debounceTimer);
                
                debounceTimer = setTimeout(() => {
                    saveState();
                }, 1000); 
            }
        });
    }
	
    // Commuta alla visualizzazione di "Verifica Progetti"
    function showVerificationView() 
	{
        sectionCreation.classList.add('hidden-section');
        sectionMonitor.classList.remove('hidden-section');
        btnToggleVerify.classList.add('hidden-section');
        btnToggleCreate.classList.remove('hidden-section');
    }

    // Commuta alla visualizzazione di "Creazione Progetto"
    function showCreationView() 
	{
        sectionMonitor.classList.add('hidden-section');
        sectionCreation.classList.remove('hidden-section');
        btnToggleCreate.classList.add('hidden-section');
        btnToggleVerify.classList.remove('hidden-section');
    }

    // Associa eventi ai pulsanti dell'header
    btnToggleVerify.addEventListener('click', showVerificationView);
    btnToggleCreate.addEventListener('click', showCreationView);

    // Gestore del form di visualizzazione progetto
	const selectProgetto=document.getElementById('idProgetto');
	    if(selectProgetto) 
		{
	        selectProgetto.addEventListener('change', (e) => {
	            const idProgettoSelezionato=parseInt(e.target.value);
	            
	            // Cerca la struttura del progetto nell'array globale pre-caricato
	            const project=loadedProjects.find(p => p.id===idProgettoSelezionato);
	            if(project) renderProjectDetails(project);
	            else console.error("Progetto non trovato localmente.");
	        });
	    }

    // Gestore del form di creazione Progetto Bulk
    const bulkForm=document.getElementById('bulk-project-form');
    if(bulkForm) bulkForm.addEventListener('submit', saveProjectBulk);

    // Listener per aggiungere WP dinamici
    document.getElementById('btn-add-wp').addEventListener('click', () => addWorkPackage());
});

// ===========================
// CARICAMENTO DATI DAL SERVER
// ===========================

/**
 * Carica l'utente e l'elenco iniziale dei progetti per popolare la select
 */
function loadInitialData() 
{
    sendAsyncRequest('GET', 'Admin', null, function(xhr) {
        if(xhr.readyState===4) 
		{
            if(xhr.status===200) 
			{
                const data=JSON.parse(xhr.responseText);					
                
                // 1. Salviamo l'intero albero gerarchico dei progetti
                loadedProjects=data.myProjects || [];

                // 2. Popola il menu a tendina dei progetti
                populateProjectsDropdown(loadedProjects);

                // 3. Popola i tecnici responsabili
                populateTechniciansSelect(data.technicians);

                // 4. Gestione del progetto di default
                if(data.defaultProjectId) 
				{
                    const selectProj=document.getElementById('idProgetto');
                    if(selectProj) selectProj.value=data.defaultProjectId;
                    
                    const defaultProj=loadedProjects.find(p => p.id===data.defaultProjectId);
                    if(defaultProj) renderProjectDetails(defaultProj);
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

/**
 * Popola la select dei responsabili tecnici con i dati forniti dal JSON di Admin
 */
function populateTechniciansSelect(technicians) 
{
    const select=document.getElementById('idResponsabile');
    if(select) 
	{
        select.innerHTML='<option value="" disabled selected>-- Seleziona un Tecnico --</option>';
        if(technicians) 
		{
            technicians.forEach(t => {
                const opt=document.createElement('option');
                opt.value=t.id; // Nel JSON è t.id
                opt.textContent=t.nomeCompleto; // Nel JSON è t.nomeCompleto
                select.appendChild(opt);
            });
        }
    }
}

// =================
// VERIFICA PROGETTI
// =================
function populateProjectsDropdown(projects) 
{
    const select=document.getElementById('idProgetto');
    if(select) 
	{
        select.innerHTML='<option value="" disabled selected>-- Scegli un progetto da verificare --</option>';
        if(projects) 
		{
            projects.forEach(p => {
                const opt=document.createElement('option');
                opt.value=p.id;
                opt.textContent=p.nomeProgetto;
                select.appendChild(opt);
            });
        }
    }
}

function renderProjectDetails(project) 
{
    const container=document.getElementById('project-details-container');
    const msgEmpty=document.getElementById('no-project-msg');
    
    if(!project) 
	{
        container.classList.add('hidden-section');
        msgEmpty.classList.remove('hidden-section');
        return;
    }

    container.classList.remove('hidden-section');
    msgEmpty.classList.add('hidden-section');

    // Allinea il valore corrente della select
    document.getElementById('idProgetto').value=project.id;

    // Aggiorna intestazione del Progetto con i dati del nuovo JSON
    document.getElementById('view-project-name').innerText=project.nomeProgetto;
    document.getElementById('view-project-state').innerText=project.stato;
    document.getElementById('view-project-hours-planned').innerText=`${project.totalProjectHours} ore`;
    document.getElementById('view-project-hours-worked').innerText=`${project.totalWorkedHours} ore`;

    // Svuota e popola la lista dei WP
    const wpListContainer=document.getElementById('view-wp-list');
    wpListContainer.innerHTML='';

    if(!project.wps || project.wps.length===0) 
	{
        wpListContainer.innerHTML=`<div class="no-tasks-msg">Il progetto non contiene ancora alcun Work Package strutturato.</div>`;
        return;
    }

    project.wps.forEach((wp, wpIdx) => {
        let tasksHtml='';

        if(!wp.tasks || wp.tasks.length===0) tasksHtml=`<div class="no-tasks-msg">Nessun task presente in questo Work Package.</div>`;
        else 
		{
            wp.tasks.forEach((t, tIdx) => {
                // Adattato alle chiavi corrette del JSON: t.idTask, t.orePreviste, t.oreLavorate
                tasksHtml += `
                    <div class="task-card task-card--row">
                        <div class="task-card-body">
                            <div class="task-label">TASK ${tIdx+1}</div>
                            <strong class="task-title">${t.nomeTask}</strong>
                            <p class="task-description">${t.descrizione || ''}</p>
                        </div>
                        <div class="task-time-box">
                            <p class="task-time-mesi">Mesi ${t.meseInizio} &rarr; ${t.meseFine}</p>
                            <p class="task-time-previste">${t.orePreviste} ore previste</p>
                            <p class="task-time-lavorate">${t.oreLavorate} ore lavorate</p>
                        </div>
                    </div>
                `;
            });
        }

        const wpCardHtml=`
            <div class="wp-card">
                <div class="wp-card-header">
                    <span class="wp-title">WP ${wpIdx+1} - ${wp.titolo}</span>
                    <span class="wp-validity-badge">Validità Mesi: ${wp.meseInizio} - ${wp.meseFine}</span>
                </div>
                <div class="task-list" style="margin-top: 10px;">
                    ${tasksHtml}
                </div>
            </div>
        `;

        wpListContainer.insertAdjacentHTML('beforeend', wpCardHtml);
    });
}

function updateIndexes() 
{
    const wpCards=document.querySelectorAll('#wp-container .wp-card');
    wpCards.forEach((wpCard, wpIdx) => {
        const wpNumber=wpIdx+1;
        const wpTitleHeader=wpCard.querySelector('.wp-header-title');
        if(wpTitleHeader) wpTitleHeader.innerText=`Work Package #${wpNumber}`;

        const taskCards=wpCard.querySelectorAll('.task-card');
        taskCards.forEach((taskCard, taskIdx) => {
            const taskNumber=taskIdx+1;
            const taskTitleHeader=taskCard.querySelector('.task-header-title');
            if(taskTitleHeader) taskTitleHeader.innerText=`Task #${taskNumber}`;
        });
    });
}

function addWorkPackage() 
{
	saveState();
    const tempWpId='wp_'+Date.now()+'_'+Math.floor(Math.random()*1000);
    const wpContainer=document.getElementById('wp-container');

    const wpHtml=`
        <div class="wp-card" id="${tempWpId}">
            <div class="section-header" style="margin-bottom: 10px;">
                <h4 class="wp-header-title" style="margin: 0; color: #0056b3;">Work Package #</h4>
                <button type="button" class="btn-danger" onclick="removeElement('${tempWpId}')">- Rimuovi WP</button>
            </div>
            
            <div style="display: flex; gap: 10px; margin-bottom: 10px; flex-wrap: wrap;">
                <div style="flex: 2; min-width: 150px;">
                    <label style="font-size: 0.85em; font-weight: bold;">Titolo WP</label>
                    <input type="text" class="form-input" required/>
                </div>
                <div style="flex: 1; min-width: 80px;">
                    <label style="font-size: 0.85em; font-weight: bold;">Mese Inizio</label>
                    <input type="number" class="form-input" min="1" required/>
                </div>
                <div style="flex: 1; min-width: 80px;">
                    <label style="font-size: 0.85em; font-weight: bold;">Mese Fine</label>
                    <input type="number" class="form-input" min="1" required/>
                </div>
            </div>

            <!-- Sotto-sezione Task -->
            <div style="margin-top: 15px; padding-left: 15px; border-left: 2px dashed #ccc;">
                <div class="section-header">
                    <h5 style="margin: 0; color: #495057;">Task associati</h5>
                    <button type="button" class="btn-secondary" style="padding: 3px 8px; font-size: 0.8em;" onclick="addTaskToWp('${tempWpId}')">+ Aggiungi Task</button>
                </div>
                <div class="tasks-container" ondragover="allowDrop(event)" ondrop="dropTask(event)" style="min-height: 50px; padding-bottom: 10px;">
                </div>
            </div>
        </div>
    `;
    
    wpContainer.insertAdjacentHTML('beforeend', wpHtml);
    updateIndexes();
    saveState(); // Cattura lo stato "appena creato" (campi vuoti), separato dalle modifiche successive
}

function addTaskToWp(wpContainerId) 
{
	saveState();
    const tempTaskId='task_'+Date.now()+'_'+Math.floor(Math.random()*1000);
    const wpCard=document.getElementById(wpContainerId);
    const tasksContainer=wpCard.querySelector('.tasks-container');

    const taskHtml=`
        <div class="task-card" id="${tempTaskId}" draggable="true" ondragstart="dragTask(event)">
            <div class="section-header" style="margin-bottom: 8px;">
                <strong class="task-header-title" style="font-size: 0.9em; color: #333;">Task #</strong>
                <button type="button" class="btn-danger" style="padding: 2px 6px; font-size: 0.75em;" onclick="removeElement('${tempTaskId}')">- Rimuovi Task</button>
            </div>
            
            <div style="display: flex; gap: 10px; margin-bottom: 8px; flex-wrap: wrap;">
                <div style="flex: 2; min-width: 150px;">
                    <label style="font-size: 0.8em;">Titolo Task</label>
                    <input type="text" class="form-input" required style="padding: 5px; font-size: 0.9em;" />
                </div>
                <div style="flex: 1; min-width: 70px;">
                    <label style="font-size: 0.8em;">Inizio</label>
                    <input type="number" class="form-input" min="1" required style="padding: 5px; font-size: 0.9em;" />
                </div>
                <div style="flex: 1; min-width: 70px;">
                    <label style="font-size: 0.8em;">Fine</label>
                    <input type="number" class="form-input" min="1" required style="padding: 5px; font-size: 0.9em;" />
                </div>
            </div>
            <div>
                <label style="font-size: 0.8em;">Descrizione</label>
                <textarea class="form-input" rows="1" required style="padding: 5px; font-size: 0.9em;"></textarea>
            </div>
        </div>
    `;
    
    tasksContainer.insertAdjacentHTML('beforeend', taskHtml);
    updateIndexes();
    saveState(); // Cattura lo stato "appena creato" (campi vuoti), separato dalle modifiche successive
}

let activePlaceholder=null;

function dragTask(event) 
{
    event.dataTransfer.setData("text/plain", event.target.id);
    event.target.classList.add('dragging');
}

// Chiamato quando il trascinamento è terminato
document.addEventListener('dragend', (event) => {
    if(event.target.classList.contains('task-card'))
        event.target.classList.remove('dragging');

    if(activePlaceholder) 
	{
        activePlaceholder.remove();
        activePlaceholder=null;
    }
});

function allowDrop(event) 
{
    event.preventDefault();
    
    const container=event.target.closest('.tasks-container');
    if(!container) return;

    // Rimuove il vecchio placeholder se presente in un altro container
    if(activePlaceholder && activePlaceholder.parentNode!==container) 
	{
        activePlaceholder.remove();
        activePlaceholder=null;
    }

    // Crea il placeholder se non esiste ancora
    if(!activePlaceholder) 
	{
        activePlaceholder=document.createElement('div');
        activePlaceholder.className='drag-placeholder';
    }

    // Trova l'elemento (task) subito sotto la posizione corrente del mouse
    const afterElement=getDragAfterElement(container, event.clientY);
    
    if(afterElement==null) container.appendChild(activePlaceholder);
    else container.insertBefore(activePlaceholder, afterElement);
}

function dropTask(event) 
{
    event.preventDefault();
    const taskId=event.dataTransfer.getData("text/plain");
    const draggedTask=document.getElementById(taskId);
    
    const container=event.target.closest('.tasks-container');
    
    if(container && draggedTask) 
	{
		saveState();
        if(activePlaceholder && container.contains(activePlaceholder)) 
		{
            container.insertBefore(draggedTask, activePlaceholder);
            activePlaceholder.remove();
            activePlaceholder=null;
        } 
		else 
            container.appendChild(draggedTask);

        updateIndexes();
    }
}


function getDragAfterElement(container, y) 
{
    const draggableElements=[...container.querySelectorAll('.task-card:not(.dragging)')];
    
    return draggableElements.reduce((closest, child) => {
        const box=child.getBoundingClientRect();
        const offset=y-box.top-box.height/2;
        
        if(offset<0 && offset>closest.offset)
            return { offset: offset, element: child };
        else
            return closest;

    }, {offset: Number.NEGATIVE_INFINITY }).element;
}

function removeElement(elementId) 
{
	saveState();
    const element=document.getElementById(elementId);
	
    if(element) 
	{
        element.remove();
        updateIndexes();
    }
}

// ==========================================
// 6. INVIO E VALIDAZIONE CREAZIONE BULK
// ==========================================

function saveProjectBulk(event) 
{
    event.preventDefault(); 

    const payload={
        nomeProgetto: document.getElementById('nomeProgetto').value,
        durata: parseInt(document.getElementById('durata').value),
        idResponsabile: document.getElementById('idResponsabile').value,
        workPackages: []
    };

    const wpCards=document.querySelectorAll('#wp-container .wp-card');
    
    if(wpCards.length===0) 
	{
        showFeedback("Attenzione: inserisci almeno un Work Package prima di salvare.", false);
        return;
    }

    let validationFailed=false;

    // 1. Iterazione sui WP per estrazione e controllo esistenza task
    wpCards.forEach(wpCard => {
        if(validationFailed) return;

        const allInputs=wpCard.querySelectorAll('.form-input');
        const wpInputs=Array.from(allInputs).filter(input => !input.closest('.task-card'));
        
        const wpData={
            nomeWP: wpInputs[0].value,
            meseInizio: parseInt(wpInputs[1].value),
            meseFine: parseInt(wpInputs[2].value),
            tasks: []
        };

        const taskCards=wpCard.querySelectorAll('.task-card');
        
        // Controllo esistenza task
        if(taskCards.length===0) 
		{
            showFeedback(`Errore: il WP "${wpData.nomeWP || 'senza nome'}" non contiene alcun Task.`, false);
            validationFailed=true;
            return;
        }

        taskCards.forEach(taskCard => {
            const taskInputs=taskCard.querySelectorAll('.form-input');
            wpData.tasks.push({
                nomeTask: taskInputs[0].value,
                meseInizio: parseInt(taskInputs[1].value),
                meseFine: parseInt(taskInputs[2].value),
                descrizione: taskInputs[3].value 
            });
        });

        payload.workPackages.push(wpData);
    });

    if(validationFailed) return;

	// 2. Validazioni logiche e temporali (Coerenza intervalli)
	    for(let wp of payload.workPackages) 
		{
	        // Coerenza interna WP
	        if(wp.meseInizio>wp.meseFine) 
			{
	            showFeedback(`Errore WP "${wp.nomeWP}": Inizio (${wp.meseInizio}) dopo Fine (${wp.meseFine}).`, false);
	            return;
	        }
	        // Coerenza WP rispetto al Progetto
	        if(wp.meseFine>payload.durata) 
			{
	            showFeedback(`Errore WP "${wp.nomeWP}": Fine (${wp.meseFine}) oltre la durata progetto (${payload.durata}).`, false);
	            return;
	        }

	        // Coerenza Task
	        for(let t of wp.tasks) 
			{
	            // Coerenza interna Task
	            if(t.meseInizio>t.meseFine) 
				{
	                showFeedback(`Errore Task "${t.nomeTask}": Inizio (${t.meseInizio}) dopo Fine (${t.meseFine}).`, false);
	                return;
	            }
	            // Coerenza Task rispetto al WP
	            if(t.meseInizio<wp.meseInizio || t.meseFine>wp.meseFine) 
				{
	                showFeedback(`Errore Task "${t.nomeTask}": Fuori intervallo WP (${wp.meseInizio}-${wp.meseFine}).`, false);
	                return;
	            }
	        }
	    }
	
    sendAsyncRequest('POST', 'DoSaveProject', payload, function(xhr) {
		if(xhr.readyState===4 && xhr.status===200) 
		{
		    showFeedback("Successo! Progetto salvato correttamente.");
		        
	        const nuovoProgetto=JSON.parse(xhr.responseText);
	        
	        loadedProjects.push(nuovoProgetto);
			
	        const select=document.getElementById('idProgetto');
	        const opt=document.createElement('option');
	        opt.value=nuovoProgetto.id;
	        opt.textContent=nuovoProgetto.nomeProgetto;
	        select.appendChild(opt);
	        
	        select.value=nuovoProgetto.id;
	        renderProjectDetails(nuovoProgetto);
	        
	        document.getElementById('bulk-project-form').reset();
	        document.getElementById('wp-container').innerHTML='';
	        addWorkPackage();
	        
	    } 
		else if(xhr.readyState===4) showFeedback("Errore durante il salvataggio: "+xhr.responseText, false);
    });
}

// ==========================================
// SISTEMA DI UNDO / REDO GLOBALE (FORM COMPLETO)
// ==========================================

const undoStack=[];
const redoStack=[];
const MAX_STATES=30;

function saveState() 
{
    const container=document.getElementById('wp-container');
    const form=document.getElementById('bulk-project-form');
    if (!container || !form) return;

    const projectData={
        nomeProgetto: document.getElementById('nomeProgetto').value,
        durata: document.getElementById('durata').value,
        idResponsabile: document.getElementById('idResponsabile').value
    };

    const inputValues=[];
    container.querySelectorAll('input, textarea').forEach((input, index) => {
        inputValues.push({
            index: index,
            value: input.value
        });
    });

    const newSnapshot={
        project: projectData,
        wpHtml: container.innerHTML,
        wpInputs: inputValues
    };

    //Verifica duplicati
    if(undoStack.length>0) 
	{
        const lastSnapshot=undoStack[undoStack.length-1];

        if(JSON.stringify(newSnapshot)===JSON.stringify(lastSnapshot))
            return; 
    }

    if(redoStack.length>0)
		redoStack.length=0;

    undoStack.push(newSnapshot);

    if(undoStack.length>MAX_STATES) 
		undoStack.shift();

    updateUndoRedoButtons();
}

/**
 * Ripristina uno snapshot specifico del form
 */
function restoreState(snapshot) 
{
    const container=document.getElementById('wp-container');
    const form=document.getElementById('bulk-project-form');
    if(!container || !form || !snapshot) return;

    document.getElementById('nomeProgetto').value=snapshot.project.nomeProgetto;
    document.getElementById('durata').value=snapshot.project.durata;
    document.getElementById('idResponsabile').value=snapshot.project.idResponsabile;

    container.innerHTML=snapshot.wpHtml;

    const inputs=container.querySelectorAll('input, textarea');
    snapshot.wpInputs.forEach(item => {
        if(inputs[item.index]) inputs[item.index].value=item.value;
    });

    updateIndexes();
    updateUndoRedoButtons();
}

function performUndo() 
{
    if (undoStack.length===0) return;

    const container=document.getElementById('wp-container');
    
    const projectData={
        nomeProgetto: document.getElementById('nomeProgetto').value,
        durata: document.getElementById('durata').value,
        idResponsabile: document.getElementById('idResponsabile').value
    };
    const currentInputValues=[];
    container.querySelectorAll('input, textarea').forEach((input, index) => {
        currentInputValues.push({ index: index, value: input.value });
    });

    redoStack.push({
        project: projectData,
        wpHtml: container.innerHTML,
        wpInputs: currentInputValues
    });

    const previousState=undoStack.pop();
    restoreState(previousState);
}

function performRedo() 
{
    if (redoStack.length===0) return;

    const container=document.getElementById('wp-container');

    // Salviamo lo stato CORRENTE in Undo prima di ripristinare il futuro
    const projectData={
        nomeProgetto: document.getElementById('nomeProgetto').value,
        durata: document.getElementById('durata').value,
        idResponsabile: document.getElementById('idResponsabile').value
    };
    const currentInputValues=[];
    container.querySelectorAll('input, textarea').forEach((input, index) => {
        currentInputValues.push({ index: index, value: input.value });
    });

    undoStack.push({
        project: projectData,
        wpHtml: container.innerHTML,
        wpInputs: currentInputValues
    });

    const nextState=redoStack.pop();
    restoreState(nextState);
}

function updateUndoRedoButtons() 
{
    const btnUndo=document.getElementById('btn-undo');
    const btnRedo=document.getElementById('btn-redo');
    
    if (btnUndo) {
        btnUndo.disabled=undoStack.length===0;
        btnUndo.style.opacity=undoStack.length===0?"0.5":"1";
        btnUndo.style.cursor=undoStack.length===0?"not-allowed":"pointer";
    }
    if (btnRedo) {
        btnRedo.disabled=redoStack.length===0;
        btnRedo.style.opacity=redoStack.length===0?"0.5":"1";
        btnRedo.style.cursor=redoStack.length===0?"not-allowed":"pointer";
    }
}