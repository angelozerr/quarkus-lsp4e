package com.redhat.quarkus.lsp4e;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher.Builder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import com.redhat.quarkus.ls.QuarkusLanguageClient;

/**
 * Starts the Quarkus LSP server inside the current JVM, and connects to it.
 */
public class QuarkusStreamConnectionProvider extends LocalStreamConnectionProvider {

	public QuarkusStreamConnectionProvider() {
		super(QuarkusPlugin.getDefault().getLog(), QuarkusPlugin.getPluginId());
	}

	@Override
	protected LocalServer launchServer(InputStream clientToServerStream, OutputStream serverToClientStream)
			throws IOException {
		JDTQuarkusLanguageServer server = new JDTQuarkusLanguageServer();
		Launcher<LanguageClient> launcher = createServerLauncher(server, clientToServerStream, serverToClientStream);
		server.setClient(launcher.getRemoteProxy());
		Future<?> launchedFuture = launcher.startListening();

		return new LocalServer(launchedFuture) {
			@Override
			public void stop() {
				super.stop();
			}
		};
	}

	/**
	 * Create a new Launcher for a language server and an input and output stream.
	 * Threads are started with the given executor service. The wrapper function is
	 * applied to the incoming and outgoing message streams so additional message
	 * handling such as validation and tracing can be included.
	 * 
	 * @param server          - the server that receives method calls from the
	 *                        remote client
	 * @param in              - input stream to listen for incoming messages
	 * @param out             - output stream to send outgoing messages
	 * @param executorService - the executor service used to start threads
	 * @param wrapper         - a function for plugging in additional message
	 *                        consumers
	 */
	public static Launcher<LanguageClient> createServerLauncher(LanguageServer server, InputStream in, OutputStream out) {
		return new Builder<LanguageClient>().setLocalService(server).setRemoteInterface(QuarkusLanguageClient.class) // Set
																														// client
																														// as
																														// Quarkus
																														// language
																														// client
				.setInput(in).setOutput(out).create();
	}
	
	@Override
	public int hashCode() {
		return 10;
	}
}