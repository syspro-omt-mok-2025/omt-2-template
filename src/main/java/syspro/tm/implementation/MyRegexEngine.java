package syspro.tm.implementation;

import syspro.tm.RegexEngine;
import syspro.tm.jit.LanguageVersion;
import syspro.tm.jit.NativeCompilerRunner;
import syspro.tm.jit.NativeLibraries;
import syspro.tm.jit.NativeLibrary;
import syspro.tm.regex.Regex;

import java.util.IdentityHashMap;
import java.util.function.Function;

public final class MyRegexEngine implements RegexEngine, Function<byte[], NativeLibrary> {
    private final IdentityHashMap<byte[], NativeLibrary> cache = new IdentityHashMap<>();

    @Override
    public boolean matches(byte[] regex, byte[] input) {
        return cache.computeIfAbsent(regex, this).callMatches(input);
    }

    @Override
    public NativeLibrary apply(byte[] regex) {
        final var regexParsed = Regex.parse(regex);
        final var regexLiteral = regexParsed.accept(new CppStdRegexConverter());
        final var code = """
                #include <iostream>
                #include <cstddef>
                #include <string_view>
                #include <regex>
                
                #ifndef __has_attribute
                  #define __has_attribute(x) 0
                #endif
                
                #ifndef LIB_EXPORT
                  #if defined(_WIN32) || defined(_WIN64)
                    #define LIB_EXPORT    __declspec(dllexport)
                  #elif (defined(__GNUC__) && ((__GNUC__ > 4) || (__GNUC__ == 4) && (__GNUC_MINOR__ > 2))) || __has_attribute(visibility)
                    #ifdef ARM
                      #define LIB_EXPORT  __attribute__((externally_visible,visibility("default")))
                    #else
                      #define LIB_EXPORT  __attribute__((visibility("default")))
                    #endif
                  #else
                    #define LIB_EXPORT
                  #endif
                #endif
                
                static const char MY_REGEX_LITERAL[] = "%s";
                static std::regex my_regex(MY_REGEX_LITERAL, sizeof(MY_REGEX_LITERAL) - 1);
                
                extern "C" LIB_EXPORT int matches(const char* input, std::size_t length)
                {
                    std::string_view input_view { input, length };
                    std::cmatch m;
                    try {
                        return std::regex_match(input_view.begin(), input_view.end(), my_regex) ? 1 : 0;
                    } catch (std::regex_error& e) {
                        std::cerr << e.what() << std::endl;
                        return 0;
                    }
                }
                """.formatted(regexLiteral);
        final var dllLibrary = NativeCompilerRunner.compile(code, LanguageVersion.Cpp17);
        if (dllLibrary == null) {
            throw new RuntimeException("Failed to compile C++ source code:\n" + code);
        }
        try {
            final var nativeLibrary = NativeLibraries.load(dllLibrary);
            nativeLibrary.setMatchesFunction("matches");
            return nativeLibrary;
        } catch (IllegalArgumentException exception) {
            // Most likely std::regex constructor has thrown a C++ exception.
            throw new RuntimeException("C++ regular expression parse failure:\n" + regexLiteral);
        }
    }
}
