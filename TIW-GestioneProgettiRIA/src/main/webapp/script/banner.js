function showFeedback(message, isSuccess=true) 
{
    const banner=document.getElementById('feedback-banner');
    const text=document.getElementById('feedback-text');
    
    if(!banner || !text) return;
    
    banner.style.display='block';
    text.innerText=message;
    
    if(isSuccess) 
	{
        banner.style.backgroundColor='#d4edda';
        banner.style.color='#155724';
        banner.style.border='1px solid #c3e6cb';
    } 
	else 
	{
        banner.style.backgroundColor='#f8d7da';
        banner.style.color='#721c24';
        banner.style.border='1px solid #f5c6cb';
    }
    
    banner.scrollIntoView({ behavior: 'smooth', block: 'center' });
	
	setTimeout(() => {
		banner.style.display="none";
	}, 2500);
}