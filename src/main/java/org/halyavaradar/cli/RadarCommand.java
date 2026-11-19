package org.halyavaradar.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "halyava-radar",
        description = "Получение и просмотр данных из внешних API.",
        synopsisHeading = "Использование: ",
        descriptionHeading = "%nОписание:%n",
        optionListHeading = "%nПараметры:%n",
        commandListHeading = "%nКоманды:%n"
)
public class RadarCommand implements Runnable {
    @CommandLine.Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Показать справку."
    )
    private boolean help;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
