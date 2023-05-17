package imageclean;

import jcli.annotations.CliCommand;
import jcli.annotations.CliOption;
import jcli.annotations.CliPositional;

import java.util.List;

@CliCommand(name = "imageclean")
public final class CliImageClean {

    @CliOption(longName = "no-recursion", description = "Disables recursive parsing")
    public boolean noRecursion;

    @CliPositional
    public List<String> paths;

}
