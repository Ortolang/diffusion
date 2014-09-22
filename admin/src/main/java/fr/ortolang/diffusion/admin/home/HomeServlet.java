package fr.ortolang.diffusion.admin.home;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class HomeServlet extends HttpServlet {
	
	private Logger logger = Logger.getLogger(HomeServlet.class.getName());

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.log(Level.FINE, "Home Servlet Called");
		getServletContext().getRequestDispatcher("/WEB-INF/pages/home.jsp").forward(request, response);
	}

}
