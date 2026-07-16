function loadUserProfile() 
{
	document.getElementById('admin-name').innerText=`${localStorage.getItem("nome")} ${localStorage.getItem("cognome")}`
	
	if(localStorage.getItem("propic"))
		document.getElementById('admin-profile-pic').src=`./img/${localStorage.getItem("propic")}`;
}