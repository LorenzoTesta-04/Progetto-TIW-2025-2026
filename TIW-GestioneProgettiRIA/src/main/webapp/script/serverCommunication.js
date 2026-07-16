function sendAsyncRequest(method, url, data, callback) 
{
    const xhr=new XMLHttpRequest();
    xhr.onreadystatechange=function() { callback(xhr); };
    xhr.open(method, url, true);
    
    if(data==null) xhr.send();
    else if(data instanceof HTMLFormElement) 
	{
        const formData=new FormData(data);
        const encodedData=new URLSearchParams(formData).toString();
        xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        xhr.send(encodedData);
    } 
	else if(typeof data==='object') 
	{
        xhr.setRequestHeader("Content-Type", "application/json");
        xhr.send(JSON.stringify(data));
    }
}