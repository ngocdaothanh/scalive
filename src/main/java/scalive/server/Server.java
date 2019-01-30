package scalive.server;

import scalive.Classpath;
import scalive.Log;
import scalive.Net;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLClassLoader;

class Server {
    static ServerSocket open(int port) throws IOException {
        Log.log("Open local port " + port + " for REPL and completer");
        return new ServerSocket(port, 1, Net.LOCALHOST);
    }

    static void run(
            ServerSocket serverSocket, String[] jarSearchDirs
    ) throws IOException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InterruptedException {
        // Accept 2 connections (blocking)
        Socket replSocket = serverSocket.accept();
        Log.log("REPL connected");

        Socket completerSocket = serverSocket.accept();
        Log.log("Completer connected");

        // Accept no further connections
        serverSocket.close();

        // Load dependency JARs
        URLClassLoader cl = (URLClassLoader) ClassLoader.getSystemClassLoader();
        loadDependencyJars(cl, jarSearchDirs);

        Runnable socketCleaner = Net.getSocketCleaner(replSocket, completerSocket);

        ILoopWithCompletion iloop = ServerRepl.run(replSocket, cl, socketCleaner);
        ServerCompleter.run(completerSocket, iloop, socketCleaner);
    }

    private static void loadDependencyJars(
            URLClassLoader cl, String[] jarSearchDirs
    ) throws IllegalAccessException, InvocationTargetException, MalformedURLException, NoSuchMethodException, ClassNotFoundException {
        // Try scala-library first
        Classpath.findAndAddJar(cl, "scala.AnyVal", jarSearchDirs, "scala-library");

        // So that we can get the actual Scala version being used
        String version = Classpath.getScalaVersion(cl);

        Classpath.findAndAddJar(cl, "scala.reflect.runtime.JavaUniverse", jarSearchDirs, "scala-reflect-"  + version);
        Classpath.findAndAddJar(cl, "scala.tools.nsc.interpreter.ILoop",  jarSearchDirs, "scala-compiler-" + version);

        Classpath.findAndAddJar(cl, "scalive.Repl", jarSearchDirs, "scalive");
    }
}
