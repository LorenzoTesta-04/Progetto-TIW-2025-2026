document.getElementById("login-button").addEventListener("click", (e) => {
    e.preventDefault();
    
    let form=e.target.closest("form");
    
    if(checkLoginParameters(form)) 
	{
        sendAsyncRequest("POST", "/TIW-GestioneProgettiRIA/DoLogin", form, function(xhr) {
            if(xhr.readyState==XMLHttpRequest.DONE) 
			{
                let response;
                try 
				{
                    response=JSON.parse(xhr.responseText);
                } 
				catch(err) 
				{
                    response={};
                }
                
                switch(xhr.status) 
				{
                    case 200:

                        localStorage.setItem("nome", response.nome);
                        localStorage.setItem("cognome", response.cognome);
                        localStorage.setItem("propic", response.profilePicturePath);
                      
                        if(response.requiresChoice) showRoleSelection(response.nome+" "+response.cognome);
                        else window.location.href=response.redirectUrl;
                        break;
						
                    case 400:
                    case 401:
                        showFeedback(response.error, false);
						form.username.value="";
						form.password.value="";
                        break;
						
                    case 500:
						showFeedback("Server error: "+response.error, false);
                        break;
						
                    default: 
						showFeedback("Unknown error.", false);
                        break;
                }
            }
        });		
    } 
	else error.update("Username and password can't be empty.");
});


const onRoleSelected=(e) => {
	
    e.preventDefault();
    let form=e.target.closest("form");
    
    sendAsyncRequest("POST", "/TIW-GestioneProgettiRIA/DoChooseRole", form, function(xhr) {
        if(xhr.readyState==XMLHttpRequest.DONE) 
		{
            let response;
            try 
			{
                response=JSON.parse(xhr.responseText);
            } 
			catch(err) 
			{
                response={};
            }
            
            if(xhr.status==200) 
			{
                window.location.href=response.redirectUrl;
            } 
			else 
				showFeedback("Impossibile impostare il ruolo selezionato.", false);
        }
    });
};

function showRoleSelection(user) 
{
    document.getElementById("user-fullname").textContent=user;
    document.getElementById("role-container").removeAttribute("style");

    document.getElementById("btn-manager").addEventListener("click", onRoleSelected);
    document.getElementById("btn-collaborator").addEventListener("click", onRoleSelected);

    document.getElementById("login-container").setAttribute("style", "display:none");
    document.getElementById("role-container").setAttribute("style", "display:block");
}

function checkLoginParameters(form) 
{
    return checkUsername(form.username.value) && checkPassword(form.password.value);
}

function checkUsername(username) 
{
    if(username==null || username=="") return false;
    return true;
}

function checkPassword(password) 
{
    if(password==null || password=="") return false;
    return true;
}