package com.redhat.quarkus.lsp4e;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

public class JDTUtils {

	public static final String PATH_SEPARATOR = "/";
	
	public static final String FILE_UNC_PREFIX = "file:////";
	
	public static IFile findFile(String uriString) {
		return (IFile) findResource(toURI(uriString),
				ResourcesPlugin.getWorkspace().getRoot()::findFilesForLocationURI);
	}
	
	public static URI toURI(String uriString) {
		if (uriString == null || uriString.isEmpty()) {
			return null;
		}
		try {
			URI uri = new URI(uriString);
			if (Platform.OS_WIN32.equals(Platform.getOS()) && URIUtil.isFileURI(uri)) {
				uri = URIUtil.toFile(uri).toURI();
			}
			return uri;
		} catch (URISyntaxException e) {
	//		JavaLanguageServerPlugin.logException("Failed to resolve "+uriString, e);
			return null;
		}
	}
	
	public static IResource findResource(URI uri, Function<URI, IResource[]> resourceFinder) {
		if (uri == null || !"file".equals(uri.getScheme())) {
			return null;
		}
		IResource[] resources = resourceFinder.apply(uri);
		if (resources.length == 0) {
			//On Mac, Linked resources are referenced via the "real" URI, i.e file://USERS/username/...
			//instead of file://Users/username/..., so we check against that real URI.
			URI realUri = FileUtil.realURI(uri);
			if (!uri.equals(realUri)) {
				uri = realUri;
				resources = resourceFinder.apply(uri);
			}
		}
		if (resources.length == 0 && Platform.OS_WIN32.equals(Platform.getOS()) && uri.toString().startsWith(FILE_UNC_PREFIX)) {
			String uriString = uri.toString();
			int index = uriString.indexOf(PATH_SEPARATOR, FILE_UNC_PREFIX.length());
			if (index > 0) {
				String server = uriString.substring(FILE_UNC_PREFIX.length(), index);
				uriString = uriString.replace(server, server.toUpperCase());
				try {
					uri = new URI(uriString);
				} catch (URISyntaxException e) {
					//JavaLanguageServerPlugin.logException(e.getMessage(), e);
				}
				resources = resourceFinder.apply(uri);
			}
		}
		switch(resources.length) {
		case 0:
			return null;
		case 1:
			return resources[0];
		default://several candidates if a linked resource was created before the real project was configured
				IResource resource = null;
				for (IResource f : resources) {
				//delete linked resource
				if (false /*JavaLanguageServerPlugin.getProjectsManager().getDefaultProject().equals(f.getProject())*/) {
					try {
						f.delete(true, null);
					} catch (CoreException e) {
	//						JavaLanguageServerPlugin.logException(e.getMessage(), e);
					}
				}
				//find closest project containing that file, in case of nested projects
					if (resource == null || f.getProjectRelativePath().segmentCount() < resource.getProjectRelativePath().segmentCount()) {
						resource = f;
				}
			}
				return resource;
		}
	}
}
