package it.polimi.tiw.progetto2025.controllers.actionPerformer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class DoLogout extends HttpServlet 
{
	private static final long serialVersionUID=1L;

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session=request.getSession(false);
		
		if(session!=null)
			session.invalidate();
		
		String path=getServletContext().getContextPath()+"/Login";
		response.sendRedirect(path);
	}

    @Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}