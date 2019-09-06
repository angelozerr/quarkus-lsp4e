package com.redhat.quarkus.lsp4e;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;

import com.redhat.quarkus.commons.QuarkusProjectInfo;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import com.redhat.quarkus.jdt.core.DocumentationConverter;
import com.redhat.quarkus.jdt.core.JDTQuarkusManager;
import com.redhat.quarkus.ls.QuarkusLanguageServer;

public class JDTQuarkusLanguageServer extends QuarkusLanguageServer {

	@Override
	public CompletableFuture<QuarkusProjectInfo> getQuarkusProjectInfo(QuarkusProjectInfoParams params) {
		return CompletableFutures.computeAsync((cancelChecker) -> {
			IProgressMonitor monitor = new NullProgressMonitor() {
				public boolean isCanceled() {
					cancelChecker.checkCanceled();
					return false;
				};
			};
			try {
				String applicationPropertiesUri = params.getUri();
				IFile file = JDTUtils.findFile(applicationPropertiesUri);
				if (file == null) {
					throw new UnsupportedOperationException(
							String.format("Cannot find IFile for '%s'", applicationPropertiesUri));
				}
				String projectName = file.getProject().getName();
				return JDTQuarkusManager.getInstance().getQuarkusProjectInfo(projectName,
						DocumentationConverter.DEFAULT_CONVERTER, monitor);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
}
