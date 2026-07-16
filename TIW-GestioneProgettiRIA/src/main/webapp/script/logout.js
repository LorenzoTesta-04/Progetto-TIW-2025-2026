function logout()
{
	sendAsyncRequest("GET", 'DoLogout', null, function(x) {
		if(x.readyState==XMLHttpRequest.DONE)
        {
        	localStorage.clear();
        	window.location.href="login.html";
        }
	});
}