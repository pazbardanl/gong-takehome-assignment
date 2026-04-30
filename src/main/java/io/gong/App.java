package io.gong;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.gong.cli.CalendarAvailabilityCli;
import io.gong.config.CalendarApplicationConfiguration;

/**
 * Loads a CSV calendar, stores it in-memory, finds joint availability for named persons.
 *
 * <p>{@code Usage: calendar.csv-path durationMinutes person [person ...]}
 *
 * <p>Example: {@code mvn exec:java -Dexec.args="path/to/calendar.csv 60 Alice Jack"}
 */
public class App {

    private static final String LOGO =
            "                                    //                                                              \n"
                    + "                      *///.      .////*         ,*.                                                 \n"
                    + "                     ,///////***////////,   *//////                                                 \n"
                    + "      ./////*,,,,,. *///////////////////////////////                                                \n"
                    + "        ,///////////////////////////////////////////          ,,                                    \n"
                    + "         *//////////////////////////////////////////////////////.                                   \n"
                    + "          .////////////////////////////////////////////////////.                                    \n"
                    + "     *////////////////////////////////////////////////////////,                                     \n"
                    + " ///////////////////**#@@@@@@@@@@%///////*/%@@@@@@@@@#/*/////(@@@@@       @@@@@@       &@@@@@@@@@@@/\n"
                    + "  .///////////////*%@@@@@@@@@@@@@@%*////%@@@@@@@@@@@@@@@#*///(@@@@@@#     @@@@@@   ,@@@@@@@@@@@@@@@@\n"
                    + "     *////////////&@@@@&/*/////////////@@@@@%*////*/&@@@@%*//#@@@@@@@@/   @@@@@@  .@@@@@@.       ,,.\n"
                    + "      .//////////%@@@@&*///((((((((//*&@@@@%*//////*(@@@@@(*/%@@@@@@@@@@# @@@@@/  @@@@@@   ,########\n"
                    + "     .///////////&@@@@%*/*/&@@@@@@@(*/@@@@@#*//////*(@@@@@(*/&@@@@ (@@@@@@@@@@@.  @@@@@@   /@@@@@@@@\n"
                    + "    *////////////%@@@@@/*//***%@@@@(/*#@@@@@//////*/&@@@@%*//@@@@@  .%@@@@@@@@@.  @@@@@@*      @@@@@\n"
                    + "  .///////////////%@@@@@@%(((#@@@@&///*#@@@@@@#((%@@@@@@#*/*(@@@@@    /@@@@@@@@.  .&@@@@@@@@(%@@@@@@\n"
                    + "///////////////////*#@@@@@@@@@@@@@%/////*(&@@@@@@@@@@&(*///*#@@@@@       @@@@@@.     (@@@@@@@@@@@@@@\n"
                    + "          .////////////*********////////////********////////*****/                                  \n"
                    + "           ./////////////////////////////////////////////.                                          \n"
                    + "          ./////////////////////////////////////////////.                                           \n"
                    + "         *//////////////////////////////////////////////.                                           \n"
                    + "        ,/////////*  *////////,/////////////////////////.                                           \n"
                    + "       ///////,,      ,/////*    *////////*        ,,,*/.                                           \n"
                    + "      *///**.           */.        ,////*.                                                          \n"
                    + "    ,//*                              *,                                                            ";

    public static void main(String[] args) {
        System.out.println(LOGO);
        System.out.println();
        System.out.println();

        ConfigurableApplicationContext ctx =
                new AnnotationConfigApplicationContext(CalendarApplicationConfiguration.class);
        try {
            ctx.getBean(CalendarAvailabilityCli.class).run(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            ctx.close();
        }
    }
}
