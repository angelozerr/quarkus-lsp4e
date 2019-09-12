package com.redhat.quarkus.lsp4e;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;

import com.redhat.quarkus.commons.QuarkusProjectInfo;
import com.redhat.quarkus.commons.QuarkusProjectInfoParams;
import com.redhat.quarkus.commons.QuarkusPropertiesScope;
import com.redhat.quarkus.jdt.core.DocumentationConverter;
import com.redhat.quarkus.jdt.core.IQuarkusPropertiesChangedListener;
import com.redhat.quarkus.jdt.core.JDTQuarkusManager;
import com.redhat.quarkus.jdt.core.QuarkusActivator;
import com.redhat.quarkus.ls.api.QuarkusLanguageClientAPI;
import com.redhat.quarkus.ls.api.QuarkusLanguageServerAPI;

public class QuarkusLanguageClient extends LanguageClientImpl implements QuarkusLanguageClientAPI {

	private static IQuarkusPropertiesChangedListener SINGLETON_LISTENER;
	
	private IQuarkusPropertiesChangedListener listener = event -> {
		((QuarkusLanguageServerAPI) getLanguageServer()).quarkusPropertiesChanged(event);
	};

	public QuarkusLanguageClient() {
		// FIXME : how to remove the listener????
		// The listener should be removed when language server is shutdown, how to manage that????
		if (SINGLETON_LISTENER != null) {
			QuarkusActivator.getDefault().removeQuarkusPropertiesChangedListener(SINGLETON_LISTENER);
		}
		SINGLETON_LISTENER = listener;		
		QuarkusActivator.getDefault().addQuarkusPropertiesChangedListener(listener);
	}

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
				QuarkusPropertiesScope scope = params.getScope();
				return JDTQuarkusManager.getInstance().getQuarkusProjectInfo(projectName, scope,
						DocumentationConverter.DEFAULT_CONVERTER, monitor);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

}
