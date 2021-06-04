package com.cherokeelessons.audio.quality.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;

import com.cherokeelessons.audio.quality.db.AppPathConfig;
import com.cherokeelessons.audio.quality.db.AudioQualityVoteDao;

@SuppressWarnings("serial")
@WebServlet(value = "/init-servlet", loadOnStartup = 1)
public class InitServlet extends HttpServlet {
	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig config = getServletConfig();
		ServletContext context = config.getServletContext();
		String contextPath = context.getContextPath();
		if (contextPath.isEmpty()) {
			contextPath = "Audio-Quality-Vote";
		} else {
			contextPath = StringUtils.strip(contextPath, "/");
		}
		System.out.println("SERVLET CONTEXT: " + contextPath);
		AppPathConfig.findConfigFile(context.getRealPath("/"), contextPath);
		System.out.println(context.getRealPath("/"));
		System.out.println(AppPathConfig.PROPERTIES_FILE.getAbsolutePath());
	}

	@Override
	public void destroy() {
		AudioQualityVoteDao.close();
		super.destroy();
	}

	private AudioQualityVoteDao dao() {
		return AudioQualityVoteDao.onDemand(AppPathConfig.TABLE_PREFIX);
	}
}
