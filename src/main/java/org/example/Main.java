package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private List<String> result;

    @Override
    public void start(Stage primaryStage) throws Exception {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        var pasta = Optional.ofNullable(chooser.showDialog(new Stage()));
        if (pasta.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Operação cancelada").showAndWait();
            Platform.exit();
            return;
        }
        result = Files.list(pasta.get().toPath())
                .parallel()
                .filter(Files::isDirectory)
                .map(path -> Map.entry(path, pathSize(path)))
                .collect(Collectors.toList())
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(this::classificarPorPasta)
                .collect(Collectors.toList());
        result.forEach(System.out::println);

        new Alert(
                Alert.AlertType.INFORMATION, "Salvar arquivos?",
                new ButtonType("SIM", ButtonBar.ButtonData.APPLY),
                new ButtonType("NÃO", ButtonBar.ButtonData.CANCEL_CLOSE)
        ).showAndWait().ifPresent(this::salvar);

        Platform.exit();
    }

    private void salvar(ButtonType buttonType) {
        if (buttonType.getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) return;
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        var pasta = Optional.ofNullable(chooser.showDialog(new Stage()));
        if (pasta.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Operação cancelada").showAndWait();
            return;
        }
        var arquivo = pasta.get().toPath().resolve("folder_scanner_log.txt");
        try (var stream = new BufferedOutputStream(new FileOutputStream(arquivo.toFile()))) {
            stream.write(String.join("\n", result).getBytes());
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String classificarPorPasta(Map.Entry<Path, Long> entry) {
        var pathSize = entry.getValue();
        String resposta;
        if (pathSize < 1_000L) resposta = pathSize.toString().concat("b");
        else if (pathSize < 1_000_000L) resposta = Long.toString(pathSize / 1_000L).concat("kb");
        else if (pathSize < 1_000_000_000L) resposta = Long.toString(pathSize / 1_000_000L).concat("Mb");
        else if (pathSize < 1_000_000_000_000L) resposta = Long.toString(pathSize / 1_000_000_000L).concat("Gb");
        else resposta = Long.toString(pathSize / 1_000_000_000_000L).concat("Tb");
        return resposta.concat(" >> ").concat(entry.getKey().toString());
    }

    private Long pathSize(Path globalPath) {
        try {
            return Files.list(globalPath)
                    .parallel()
                    .mapToLong((localPath) -> {
                        try {
                            if (Files.isDirectory(localPath)) return pathSize(localPath);
                            return Files.size(localPath);
                        } catch (IOException e) {
                            return tratarExcecaoPathSize(e, localPath);
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return tratarExcecaoPathSize(e, globalPath);
        }
    }

    private Long tratarExcecaoPathSize(IOException e, Path localPath) {
        if (e instanceof AccessDeniedException) {
            System.out.println("Acesso negado => " + localPath);
            return 0L;
        }
        if (e instanceof FileSystemException) {
            System.out.println("Arquivo de sitema => " + localPath);
            return 0L;
        }
        e.printStackTrace();
        return 0L;
    }
}
