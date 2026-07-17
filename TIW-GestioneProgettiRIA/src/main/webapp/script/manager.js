var loadedData={
    managedProjects: [],
    collaborators: [],
	managedCollaborators: []
};

document.addEventListener('DOMContentLoaded', function() {
	loadUserProfile();	
    loadInitialData();
    initializeSectionNavigation();
    setupAllocationListeners();
    setupMonitorListeners();
});

function loadInitialData() 
{
    sendAsyncRequest('GET', 'Manager', null, function(xhr) {
        if(xhr.readyState===4) 
		{
            if(xhr.status===200) 
			{
                try 
				{
                    var response=JSON.parse(xhr.responseText);
                    loadedData.managedProjects=response.managedProjects || [];
                    loadedData.collaborators=response.collaborators || [];
					loadedData.managedCollaborators=response.managedCollaborators || [];
                    
                    populateProjectSelectors();
                    populateCollaboratorSelector();
                }
				catch(e) 
				{
                    console.error("Errore nel parsing del JSON di risposta:", e);
                }
				
				document.body.style.visibility='visible';
	        } 
			else
			{
				localStorage.clear();
				window.location.href="login.html";
			}
        }
    });
}

function initializeSectionNavigation() 
{
    var sections={
        allocation: document.getElementById('section-allocation'),
        monitorProj: document.getElementById('section-monitor-projects'),
        monitorCollab: document.getElementById('section-monitor-collaborators')
    };

    var buttons={
        allocation: document.getElementById('btn-show-allocation'),
        monitorProj: document.getElementById('btn-show-monitor-proj'),
        monitorCollab: document.getElementById('btn-show-monitor-collab'),
        btnBack: document.getElementById('btn-back-to-allocation')
    };

    function deactivateAll() 
	{
        var secKeys=Object.keys(sections);
        for(var i=0; i<secKeys.length; i++) 
		{
            var sec=sections[secKeys[i]];
            if(sec) 
				sec.classList.add('hidden-section');
        }
        
        var btnKeys=['allocation', 'monitorProj', 'monitorCollab'];
        for(var j=0; j<btnKeys.length; j++) 
		{
            var btn=buttons[btnKeys[j]];
            if(btn) 
				btn.classList.remove('btn-action--active');
        }
    }

    buttons.monitorProj.addEventListener('click', function() {
        deactivateAll();
        
        sections.monitorProj.classList.remove('hidden-section');
        buttons.monitorProj.classList.add('btn-action--active');
        
        buttons.monitorProj.style.display='none';
        buttons.monitorCollab.style.display='inline-block';
        buttons.btnBack.style.display='inline-block';

        var selectProj=document.getElementById('idProgetto');
        if(selectProj && selectProj.value) 
		{
            var selectProjMon=document.getElementById('idProgettoMonitor');
            if(selectProjMon)
			{
                selectProjMon.value=selectProj.value;
                handleMonitorProjectSelection(parseInt(selectProj.value));
            }
        }
    });


    buttons.monitorCollab.addEventListener('click', function() {
        deactivateAll();
        
        sections.monitorCollab.classList.remove('hidden-section');
        buttons.monitorCollab.classList.add('btn-action--active');
        
        buttons.monitorCollab.style.display='none';
        buttons.monitorProj.style.display='inline-block';
        buttons.btnBack.style.display='inline-block';
    });

    function showAllocationView() 
	{
        deactivateAll();
        
        sections.allocation.classList.remove('hidden-section');
        if(buttons.allocation) 
			buttons.allocation.classList.add('btn-action--active');
        
        buttons.monitorProj.style.display='inline-block';
        buttons.monitorCollab.style.display='inline-block';
        
        buttons.btnBack.style.display='none';
    }

    buttons.btnBack.addEventListener('click', function() {
        showAllocationView();
    });

    if(buttons.allocation) 
	{
        buttons.allocation.addEventListener('click', function() {
            showAllocationView();
        });
    }

    showAllocationView();
}

function populateProjectSelectors() 
{
    var selectAlloc=document.getElementById('idProgetto');
    var selectMon=document.getElementById('idProgettoMonitor');
    var baseOpt='<option value="" disabled selected>-- Scegli un Progetto --</option>';

    if(selectAlloc) 
	{
        selectAlloc.innerHTML=baseOpt;
        loadedData.managedProjects.forEach(function(p) {
			if(p.stato==='CREATO') 
			{
	            var opt=document.createElement('option');
	            opt.value=p.idProgetto;
	            opt.textContent=p.nome;
	            selectAlloc.appendChild(opt);
			}
        });
    }

    if(selectMon) 
	{
        selectMon.innerHTML=baseOpt;
        loadedData.managedProjects.forEach(function(p) {
            var opt=document.createElement('option');
            opt.value=p.idProgetto;
            opt.textContent=p.nome;
            selectMon.appendChild(opt);
        });
    }
}

//Le info per i collaboratori di cui sono responsabile vengono prese da collaborators
function populateCollaboratorSelector() 
{
    var selectCollab=document.getElementById('idCollaboratoreMonitor');
    if(!selectCollab)return;
    
    selectCollab.innerHTML='<option value="" disabled selected>-- Scegli un Collaboratore --</option>';

    loadedData.managedCollaborators.forEach(function(managedId) {
        var collaboratorInfo=loadedData.collaborators.find(function(c) {
            return c.id===managedId;
        });

        if(collaboratorInfo)
		{
            var opt=document.createElement('option');
            opt.value=collaboratorInfo.id;
            opt.textContent=collaboratorInfo.cognome+' '+collaboratorInfo.nome;
            selectCollab.appendChild(opt);
        }
    });
}

function setupAllocationListeners() 
{
    var selectProj=document.getElementById('idProgetto');
    var selectWp=document.getElementById('codiceWP');
    var btnStart=document.getElementById('btn-start-project');

    if(selectProj) 
        selectProj.addEventListener('change', function(e) {
            var idVal=parseInt(e.target.value);
            if(!isNaN(idVal))
                handleProjectSelection(idVal);

        });

    if(selectWp) 
        selectWp.addEventListener('change', function(e) {
            if(selectProj) 
			{
                var idProj=parseInt(selectProj.value);
                var wpCode=parseInt(e.target.value);
                if(!isNaN(idProj) && !isNaN(wpCode))
                    renderAllocationTasks(idProj, wpCode);
            }
        });

    if(btnStart) 
        btnStart.addEventListener('click', function() {
            if(selectProj) 
			{
                var idProj=parseInt(selectProj.value);
                if(!isNaN(idProj))
                    handleStartProject(idProj);
            }
        });
}

//Allocazione task
function handleProjectSelection(projectId, forceSelectWpCode) 
{
    if(forceSelectWpCode===undefined) 
		forceSelectWpCode=null;

    var project=loadedData.managedProjects.find(function(p) {return p.idProgetto===projectId;});
    
    var statusWrapper=document.getElementById('project-status-info-wrapper');
    var statusInfo=document.getElementById('project-status-info');
    var selectWpContainer=document.getElementById('wp-select-container');
    var selectWp=document.getElementById('codiceWP');
    var btnStartContainer=document.getElementById('project-start-container');
    var btnStart=document.getElementById('btn-start-project');
    var notCreatedMsg=document.getElementById('project-not-creato-msg');
    var tasksContainer=document.getElementById('tasks-allocation-container');
    var noWpMsg=document.getElementById('no-wp-selected-msg');

    // Reset iniziale dello stato visuale
    if(statusWrapper) statusWrapper.classList.add('hidden-section');
    if(selectWpContainer) selectWpContainer.classList.add('hidden-section');
    if(btnStartContainer) btnStartContainer.classList.add('hidden-section');
    if(notCreatedMsg) notCreatedMsg.classList.add('hidden-section');
    if(tasksContainer) tasksContainer.classList.add('hidden-section');
    if(noWpMsg) noWpMsg.classList.remove('hidden-section');

    if(!project)
		return;

    if(statusWrapper && statusInfo) 
	{
        statusInfo.innerHTML=`<strong>${project.stato}</strong> | <strong>Durata:</strong> ${project.durata} mesi`;
        statusWrapper.classList.remove('hidden-section');
    }

    if(project.stato==='CREATO') 
	{
        if(btnStartContainer && btnStart) 
		{
            btnStartContainer.classList.remove('hidden-section');
            if(project.isAssignable) 
			{
                btnStart.removeAttribute('disabled');
                btnStart.classList.remove('btn-assign--disabled');
                btnStart.title="Procedi con l'assegnazione";
            } 
			else 
			{
                btnStart.setAttribute('disabled', 'true');
                btnStart.classList.add('btn-assign--disabled');
                btnStart.title="Impossibile assegnare: verifica i requisiti";
            }
        }

        // Popola il menu dei Work Package
        if(selectWp) 
		{
            selectWp.innerHTML='<option value="" disabled selected>-- Scegli un Work Package --</option>';
            if(project.WP && project.WP.length>0) 
			{
                project.WP.forEach(function(wp) {
                    var opt=document.createElement('option');
                    opt.value=wp.idWp;
                    opt.textContent=wp.TitoloWP;
                    selectWp.appendChild(opt);
                });
                if(selectWpContainer) 
					selectWpContainer.classList.remove('hidden-section');
            } 
			else 
                if(statusWrapper && statusInfo)
                    statusInfo.innerHTML+=" <span style='color: #d9534f;'>(Nessun WP configurato dall'Amministratore)</span>";
        }
        
        if(forceSelectWpCode && selectWp)
		{
            selectWp.value=forceSelectWpCode;
            renderAllocationTasks(projectId, forceSelectWpCode);
		}

    } 
	else
    {

        if(notCreatedMsg) notCreatedMsg.classList.remove('hidden-section');
        if(noWpMsg) noWpMsg.classList.add('hidden-section');
    }
}

function renderAllocationTasks(projectId, wpCode) 
{
    var project=loadedData.managedProjects.find(function(p) { return p.idProgetto===projectId; });
    var wp=null;
	
    if(project && project.WP)
        wp=project.WP.find(function(w) { return w.idWp===wpCode; });

    var tasksContainer=document.getElementById('tasks-allocation-container');
    var tasksList=document.getElementById('allocation-tasks-list');
    var noWpMsg=document.getElementById('no-wp-selected-msg');

    if(!wp) 
    {
        if(tasksContainer) tasksContainer.classList.add('hidden-section');
        if(noWpMsg) noWpMsg.classList.remove('hidden-section');
        return;
    }

    if(noWpMsg) noWpMsg.classList.add('hidden-section');
    if(tasksContainer) tasksContainer.classList.remove('hidden-section');

    var titleElem=document.getElementById('allocation-wp-title');
    if(titleElem) titleElem.textContent=`${wp.NumeroOrdine}. ${wp.TitoloWP}`; 

    var validityElem=document.getElementById('allocation-wp-validity');
    if(validityElem)
        validityElem.innerHTML=`Validità WP: Mese <strong>${wp.MeseInizio}</strong> &rarr; Mese <strong>${wp.MeseFine}</strong>`;

    if(!tasksList) return;
    tasksList.innerHTML='';

    if(!wp.Task || wp.Task.length===0) 
    {
        tasksList.innerHTML='<div class="no-tasks-msg">Nessun task pianificato in questo Work Package.</div>';
        return;
    }

    wp.Task.forEach(function(t) {
        var card=document.createElement('div');
        card.className='task-card';

        // Collaboratori
        var collabHtml='';
        loadedData.collaborators.forEach(function(c) {
            var isChecked='';
            if(t.Collab && t.Collab.some(function(col) { return col.id===c.id; }))
                isChecked='checked';
            
            collabHtml+=`
                <div class="checkbox-item">
                    <input type="checkbox" name="idCollaboratori" id="collab_${t.idTask}_${c.id}" value="${c.id}" ${isChecked} />
                    <label for="collab_${t.idTask}_${c.id}">${c.cognome} ${c.nome}</label>
                </div>`;
        });

        //Box mesi
        var monthsHtml='';
        for(var m=t.MeseInizio; m<=t.MeseFine; m++) 
		{
            var monthIndex=m-t.MeseInizio;
            var plannedHours=0;
            
            if(t.ore && t.ore[monthIndex])
                plannedHours=t.ore[monthIndex].prevista || 0;

            monthsHtml+=`
                <div class="month-box">
                    <label for="ore_task_${t.idTask}_mese_${m}" class="month-label">Mese ${m}</label>
                    <input type="number" id="ore_task_${t.idTask}_mese_${m}" name="ore_task_${t.idTask}_mese_${m}" value="${plannedHours}" min="0" placeholder="0" required class="month-input" />
                </div>`;
        }

        card.innerHTML=`
            <div class="task-card-header">
                <div class="task-label">TASK</div>
                <strong class="task-title">${wp.NumeroOrdine}.${t.NumeroOrdine} ${t.TitoloTask}</strong>
                <p class="task-description">${t.Descrizione || ''}</p>
            </div>
            <div class="allocation-form-wrapper">
                <form id="form-alloc-task-${t.idTask}" class="allocation-form" autocomplete="off">
                    <div class="collaborators-section">
                        <label class="form-section-label">Collaboratori del Task</label>
                        <p class="form-section-desc">Seleziona i collaboratori da associare a questa pianificazione.</p>
                        <div class="checkbox-list-container">${collabHtml}</div>
                    </div>
                    <div class="hours-section">
                        <div>
                            <label class="form-section-label">Ore Lavorative Mensili</label>
                            <p class="form-section-desc">Includi la stima oraria prevista per ciascun mese di attività del task.</p>
                            <div class="months-grid">${monthsHtml}</div>
                        </div>
                        <button type="submit" class="btn-save-allocation">Salva Allocazione Task</button>
                    </div>
                </form>
            </div>`;

        tasksList.appendChild(card);

        var formElem=document.getElementById(`form-alloc-task-${t.idTask}`);
        if(formElem) 
		{
            formElem.addEventListener('submit', function(event) {
                event.preventDefault();
                submitAllocationForm(t.idTask, wp.idWp, projectId);
            });
        }
    });
}

//Invio allocazione, in caso di successo il server restituisce il json del progetto 
function submitAllocationForm(taskId, wpCode, projectId) 
{
    var form=document.getElementById('form-alloc-task-'+taskId);
    if(!form) return;

    var checkboxes=form.querySelectorAll('input[name="idCollaboratori"]:checked');
    var selectedCollabs=Array.from(checkboxes).map(function(cb) { return parseInt(cb.value); });

    if(selectedCollabs.length===0) 
	{
        showFeedback("Errore: devi selezionare almeno un collaboratore per il task.", false);
        return;
    }

    var payload={
        idProgetto: projectId,
        codiceWP: wpCode,
        idTask: taskId,
        idCollaboratori: selectedCollabs,
        orePreviste: {}
    };

    var inputs=form.querySelectorAll('.month-input');
    var validationPassed=true;
    
    for(var i=0; i<inputs.length; i++) 
	{
        var input=inputs[i];
        var hoursVal=parseInt(input.value);
        var nameAttr=input.name; 
        var monthNum=nameAttr.split('_').pop();

        if(isNaN(hoursVal) || hoursVal<=0) 
		{
            showFeedback('Errore: Le ore indicate per il mese '+monthNum+' non sono valide.', false);
            validationPassed=false;
            break; 
        }
        payload.orePreviste[monthNum]=hoursVal;
    }

    if(!validationPassed) return;

	sendAsyncRequest('POST', 'DoTaskAllocation', payload, function(xhr) {
        if(xhr.readyState===4)
		{
            if(xhr.status===200) 
			{
                try
				{
                    var response=JSON.parse(xhr.responseText);
                    
                    var projIndex=loadedData.managedProjects.findIndex(function(p) {
                        return p.idProgetto===projectId;
                    });
                    
                    if(projIndex!==-1 && response.Project)
                        loadedData.managedProjects[projIndex]=response.Project;

                    
                    if(response.managedCollaborators) 
					{
                        loadedData.managedCollaborators=response.managedCollaborators;
                        populateCollaboratorSelector();
                    }

                    showFeedback("Allocazione salvata con successo!", true);
                    
                    handleProjectSelection(projectId, wpCode);
                    
                } 
				catch(e) 
				{
                    showFeedback("Allocazione salvata, ma si è verificato un errore nell'aggiornamento dei dati locali.", false);
                }
            } 
			else
                showFeedback('Errore nel salvataggio: '+xhr.responseText, false);
        }
    });
}

//Assegna progetto
function handleStartProject(projectId) 
{
    var payload={ idProgetto: projectId };

    sendAsyncRequest('POST', 'DoStartProject', payload, function(xhr) 
	{
        if(xhr.readyState===4) 
		{
            if(xhr.status===200) 
			{
                showFeedback("Progetto avviato e assegnato con successo!", true);
				
				var project=loadedData.managedProjects.find(function(p) { 
                    return p.idProgetto===projectId; 
                });

                if(project) 
                {
                    project.stato='ASSEGNATO';
                    
					populateProjectSelectors();
					
					var selectProj=document.getElementById('idProgetto');
					var selectWp=document.getElementById('codiceWP');
					if(selectProj) selectProj.value=""; // Svuota la selezione del progetto
					if(selectWp) selectWp.innerHTML='<option value="" disabled selected>-- Scegli un Work Package --</option>';
					handleProjectSelection(null);

                    var selectMon=document.getElementById('idProgettoMonitor');
                    if(selectMon) 
						selectMon.value=projectId;
					
					handleMonitorProjectSelection(projectId);

                    var btnMonProj=document.getElementById('btn-show-monitor-proj');
                    if(btnMonProj) 
						btnMonProj.click();
                }
            } 
			else
                showFeedback('Errore durante l\'avvio del progetto: '+xhr.responseText, false);
        }
    });
}

//Monitor progetti
function setupMonitorListeners() 
{
    var selectMon=document.getElementById('idProgettoMonitor');
    var selectCollabMon=document.getElementById('idCollaboratoreMonitor');
    var btnConclude=document.getElementById('btn-conclude-project');

    if(selectMon) 
	{
        selectMon.addEventListener('change', function(e) {
            var val=parseInt(e.target.value);
            if(!isNaN(val)) 
                handleMonitorProjectSelection(val);
        });
    }

    if(selectCollabMon) 
	{
        selectCollabMon.addEventListener('change', function(e) {
            var val=parseInt(e.target.value);
            if(!isNaN(val))
                handleMonitorCollaboratorSelection(val);
        });
    }

    if(btnConclude && selectMon) 
	{
        btnConclude.addEventListener('click', function() {
            var projId=parseInt(selectMon.value);
            if(!isNaN(projId))
                handleConcludeProject(projId);
        });
    }
}

function handleMonitorProjectSelection(projectId) 
{
    var project=loadedData.managedProjects.find(function(p) { return p.idProgetto===projectId; });
    var detailsContainer=document.getElementById('project-monitor-details');
    var msgEmpty=document.getElementById('no-monitor-project-msg');
    var concludeBtnContainer=document.getElementById('project-conclude-container');

    if(!project) 
	{
        if(detailsContainer) detailsContainer.classList.add('hidden-section');
        if(msgEmpty) msgEmpty.classList.remove('hidden-section');
        if(concludeBtnContainer) concludeBtnContainer.classList.add('hidden-section');
        return;
    }

    if(msgEmpty) msgEmpty.classList.add('hidden-section');
    if(detailsContainer) detailsContainer.classList.remove('hidden-section');

    var stateElem=document.getElementById('mon-proj-state');
    if(stateElem) stateElem.textContent=project.stato;

    var durElem=document.getElementById('mon-proj-duration');
    if(durElem) durElem.textContent=project.durata+' Mesi';

    var projectSummary=calculateProjectHours(project);

    var planElem=document.getElementById('mon-proj-planned-hours');
    if(planElem) planElem.textContent=projectSummary.totalPlanned+'h';

    var workElem=document.getElementById('mon-proj-worked-hours');
    if(workElem) workElem.textContent=projectSummary.totalWorked+'h';

    //Concludi
    if(concludeBtnContainer)
		if(projectSummary.canConclude) concludeBtnContainer.classList.remove('hidden-section');
       	else concludeBtnContainer.classList.add('hidden-section');

    renderProjectMatrixTable(project);
}

//Generazione tabella riassuntiva progetto
function renderProjectMatrixTable(project) 
{
    const table=document.getElementById('project-monitor-table');
    if(!table) 
		return;
    
    table.textContent='';
    const duration=project.durata;

    if(!project.WP || project.WP.length===0) 
	{
        const tr=document.createElement('tr');
        const td=document.createElement('td');
        td.colSpan=2+(duration*2);
        td.textContent='Nessun dato strutturato nel progetto.';
        tr.appendChild(td);
        table.appendChild(tr);
        return;
    }

    table.appendChild(createMonitorTableHeader(duration));
    table.appendChild(createMonitorTableBody(project.WP, duration));
}

function createMonitorTableHeader(duration) 
{
    const thead=document.createElement('thead');
    const fragment=document.createDocumentFragment();

    const tr1=document.createElement('tr');
    let headerRow1='<th rowspan="2" style="min-width: 150px;">Work Package</th><th rowspan="2" style="min-width: 180px;">Task</th>';
    for(let m=1; m<=duration; m++) 
		headerRow1+=`<th colspan="2">M${m}</th>`;

    headerRow1+='</tr>';
    tr1.innerHTML=headerRow1;
    fragment.appendChild(tr1);

    const tr2=document.createElement('tr');
    let headerRow2='';
    for(let m2=1; m2<=duration; m2++)
        headerRow2+='<th class="th-previste">P</th><th class="th-lavorate">L</th>';
    
    headerRow2+='</tr>';
    tr2.innerHTML=headerRow2;
    fragment.appendChild(tr2);

    thead.appendChild(fragment);
    return thead;
}

function createMonitorTableBody(wps, duration) 
{
    const tbody=document.createElement('tbody');
    const fragment=document.createDocumentFragment();

    wps.forEach(wp => {
        const tasks=wp.Task || [];
        const totalWpTasks=tasks.length;
        
        if(totalWpTasks===0) 
		{
            const tr=document.createElement('tr');
            tr.innerHTML=`
                <td class="td-wp-name" style="font-weight: bold; background: #eef4fc;">${wp.NumeroOrdine}. ${wp.TitoloWP}</td>
                <td colspan="${1+(duration*2)}">Nessun task presente.</td>`;
            fragment.appendChild(tr);
            return;
        }

        tasks.forEach((task, tIdx) => {
            const tr=document.createElement('tr');
            let rowHtml='';
            
            if(tIdx===0)
                rowHtml+=`<td rowspan="${totalWpTasks}" class="td-wp-name" style="font-weight: bold; background: #eef4fc; vertical-align: middle;">${wp.NumeroOrdine}. ${wp.TitoloWP}</td>`;

            rowHtml+=`<td style="text-align: left; padding-left: 10px;">${wp.NumeroOrdine}.${task.NumeroOrdine} ${task.TitoloTask}</td>`;
            rowHtml+=createMonitorMonthlyCellsHtml(task, duration);

            tr.innerHTML=rowHtml;
            fragment.appendChild(tr);
        });
    });

    tbody.appendChild(fragment);
    return tbody;
}

function createMonitorMonthlyCellsHtml(task, duration) 
{
    let cellsHtml='';
    const oreArray=task.ore || [];

    // Recuperiamo in sicurezza gli indici numerici inviati da Java
    const meseInizio=parseInt(task.MeseInizio);
    const meseFine=parseInt(task.MeseFine);

    for(let monthIdx=1; monthIdx<=duration; monthIdx++) 
	{
        let pVal='-';
        let lVal='-';

        if(!isNaN(meseInizio) && !isNaN(meseFine) && monthIdx>=meseInizio && monthIdx<=meseFine) 
		{
            var arrayIndex=monthIdx-meseInizio;
            var datiMese=oreArray[arrayIndex];
            
            if(datiMese!==undefined) 
			{
                pVal=datiMese.prevista!==undefined?datiMese.prevista:0;
                lVal=datiMese.lavorata!==undefined?datiMese.lavorata:0;
            }
        }
        
        cellsHtml+=`<td class="task-time-previste">${pVal}</td><td class="task-time-lavorate">${lVal}</td>`;
    }

    return cellsHtml;
}

//Termina progetto
function handleConcludeProject(projectId) 
{
    var payload={ idProgetto: projectId };

    sendAsyncRequest('POST', 'DoConcludeProject', payload, function(xhr) {
        if(xhr.readyState===4) 
		{
            if(xhr.status===200)
			{
                showFeedback("Progetto concluso con successo!", true);
				var project=loadedData.managedProjects.find(function(p) { 
                    return p.idProgetto===projectId; 
                });

                if(project) 
				{
                    project.stato='CONCLUSO';
                    
                    var concludeBtnContainer=document.getElementById('project-conclude-container');
                    if(concludeBtnContainer)
                        concludeBtnContainer.classList.add('hidden-section');

                    var stateElem=document.getElementById('mon-proj-state');
                    if(stateElem)
                        stateElem.textContent=project.stato;
                }
            }
			else
                showFeedback('Errore nella conclusione del progetto: '+xhr.responseText, false);
        }
    });
}

//Monitor Collaboratori
function handleMonitorCollaboratorSelection(collaboratorId) 
{
    var container=document.getElementById('collaborator-projects-container');
    var details=document.getElementById('collaborator-monitor-details');
    var msgEmpty=document.getElementById('no-monitor-collab-msg');

    if(!container) return;
    container.innerHTML='';

    if(!collaboratorId || isNaN(collaboratorId)) 
	{
        if (details) details.classList.add('hidden-section');
        if (msgEmpty) msgEmpty.classList.remove('hidden-section');
        return;
    }

    if(msgEmpty) msgEmpty.classList.add('hidden-section');
    if(details) details.classList.remove('hidden-section');

    //Filtra i progetti in cui il collaboratore è allocato ad almeno un Task
    var activeProjects=loadedData.managedProjects.filter(function(p) {
        return p.WP && p.WP.some(function(wp) {
            return wp.Task && wp.Task.some(function(t) {
                return t.Collab && t.Collab.some(function(c) { return c.id===collaboratorId; });
            });
        });
    });

    if(activeProjects.length===0)
	{
        container.innerHTML='<div class="info-warning">Il collaboratore selezionato non ha task attivi nei tuoi progetti.</div>';
        return;
    }

    //Tabella
    activeProjects.forEach(function(p) {
        var block=document.createElement('div');
        block.className='collab-project-block';

        var tableDuration=p.durata;
        var thMonths='';
        for(var m=1; m <= tableDuration; m++) 
            thMonths+=`<th class="td-durata">M${m}</th>`;

        var bodyHtml='';

        //WP
        var relevantWps=p.WP.filter(function(wp) {
            return wp.Task && wp.Task.some(function(t) {
                return t.Collab && t.Collab.some(function(c) { return c.id===collaboratorId; });
            });
        });

        relevantWps.forEach(function(wp) {

            var collabTasksInWp=wp.Task.filter(function(t) {
                return t.Collab && t.Collab.some(function(c) { return c.id===collaboratorId; });
            });
            var totalTasks=collabTasksInWp.length;

            collabTasksInWp.forEach(function(task, tIdx) {
                var row='<tr>';

                if(tIdx===0)
                    row+=`<td rowspan="${totalTasks}" class="td-wp-name" style="font-weight: bold; background: #eef4fc; vertical-align: middle;">${wp.NumeroOrdine}. ${wp.TitoloWP}</td>`;

                row+=`<td style="text-align: left; padding-left: 10px;">${wp.NumeroOrdine}.${task.NumeroOrdine} ${task.TitoloTask}</td>`;

                var meseInizio=parseInt(task.MeseInizio);
                var meseFine=parseInt(task.MeseFine);
                var oreArray=task.ore || [];

                for (var monthIdx=1; monthIdx <= tableDuration; monthIdx++) 
				{
                    var lVal='-';
                    if(!isNaN(meseInizio) && !isNaN(meseFine) && monthIdx>=meseInizio && monthIdx<=meseFine) 
						{
                        var arrayIndex=monthIdx-meseInizio;
                        var datiMese=oreArray[arrayIndex];
                        if(datiMese !== undefined && datiMese.lavorata!==undefined)
                            lVal=datiMese.lavorata;
						else
                            lVal=0;
                    }
                    row+=`<td class="task-time-lavorate">${lVal}</td>`;
                }
                row+='</tr>';
                bodyHtml+=row;
            });
        });

        block.innerHTML=`
            <div class="table-box table-box--blue" style="overflow: visible; margin-bottom: 25px;">
                <div class="project-info-header">
                    <h4 style="margin: 0 0 5px 0; font-size: 1.2rem;">Progetto: <span style="font-weight: bold;">${p.nome}</span></h4>
                    <div style="display: flex; gap: 20px; font-size: 0.9rem; color: #555;">
                        <div><strong>Stato Operativo:</strong> ${p.stato}</div>
                        <div><strong>Durata Totale:</strong> ${p.durata} Mesi</div>
                    </div>
                </div>
                <div class="responsive-table-wrapper" style="width: 100%; overflow-x: auto; padding: 15px;">
                    <table border="1" style="border-collapse: collapse; width: 100%; text-align: center; border: 1px solid #dce4ec;">
                        <thead>
                            <tr style="background: #f8fafc;">
                                <th style="min-width: 150px; padding: 8px;">Work Package</th>
                                <th style="min-width: 180px; padding: 8px;">Task Assegnato</th>
                                ${thMonths}
                            </tr>
                        </thead>
                        <tbody>${bodyHtml}</tbody>
                    </table>
                </div>
            </div>`;
            
        container.appendChild(block);
    });
}

//Utility
function calculateProjectHours(project) 
{
    var summary={
        totalPlanned: 0,
        totalWorked: 0,
        canConclude: (project && project.stato==='ASSEGNATO')
    };

    if(!project || !project.WP || project.WP.length===0) 
	{
        summary.canConclude=false;
        return summary;
    }

    project.WP.forEach(function(wp) {
        var tasks=wp.Task || [];
        if(tasks.length===0)
            summary.canConclude=false;
        else 
		{
            tasks.forEach(function(t) {
                var taskPlannedTotal=0;
                var taskWorkedTotal=0;

                if(Array.isArray(t.ore)) 
				{
                    t.ore.forEach(function(coppia) {
                        taskPlannedTotal+=parseInt(coppia.prevista) || 0;
                        taskWorkedTotal+=parseInt(coppia.lavorata) || 0;
                    });
                }

                // Accumuliamo nel totale del progetto
                summary.totalPlanned+=taskPlannedTotal;
                summary.totalWorked+=taskWorkedTotal;

                // Il task si considera concluso solo se le ore lavorate totali soddisfano le previste
                if(taskWorkedTotal<taskPlannedTotal) 
                    summary.canConclude=false;
            });
        }
    });

    return summary;
}