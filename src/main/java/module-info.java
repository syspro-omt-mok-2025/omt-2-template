import syspro.tm.implementation.MyConfiguration;
import syspro.tm.implementation.MyRegexEngine;

module RegexImplementation {
    exports syspro.tm.implementation;
    requires syspro.tm.RegexApp;
    provides syspro.tm.ConfigurationProvider with MyConfiguration;
    provides syspro.tm.RegexEngine with MyRegexEngine;
}