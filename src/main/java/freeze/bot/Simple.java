package freeze.bot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simple {
    private static final Logger LOGGER = LoggerFactory.getLogger(Simple.class);

    public static void main(String[] args) {
        String token = null;
        try(BufferedReader reader = Files.newBufferedReader(new File("bot-token.txt").toPath())) {
            token = reader.readLine().strip();
        } catch(Exception ioe) {
            LOGGER.error("Cannot read bot token: ", ioe);
            System.exit(1);
        }
        DiscordClient.create(token).gateway()
            .withGateway(client -> client.on(ReadyEvent.class)
                         .flatMap(x -> withGatewayClient(client))
                         .doOnError(error -> LOGGER.error("gateway error: ", error)))
            .doOnError(error -> {
                    LOGGER.error("bot error: ", error);
                    System.exit(1);
                })
            .block();
    }

    private static Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        try {
            String commandName = event.getCommandName();
            if(!commandName.equals("fail"))
                return event.reply("Unsupported command /"+commandName).withEphemeral(true);
            return event.reply("Press button many times to freeze bot.")
                .withComponents(ActionRow.of(Button.danger("fail:0", "freeze bot (0 presses)")));
        } catch(Exception e) {
            LOGGER.error("Unhandled exception in handleChatCommand: ", e);
            throw e;
        }
    }

    private static Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        try {
            String[] fields = event.getCustomId().split(":");
            String commandName = fields[0];
            int count = Integer.parseInt(fields[1]);
            if(!commandName.equals("fail"))
                return event.reply("Unsupported command /"+commandName).withEphemeral(true);
            ActionRow row = ActionRow.of(Button.danger("fail:" + (count + 1), "freeze bot (" + (count+1) + " presses)"));
            return event.deferEdit().concatWith(Mono.defer(() -> {
                        return event.editReply().withComponents(row).then();
                        })).then();
        } catch(Exception e) {
            LOGGER.error("Unhandled exception in handleButtonInteraction: ", e);
            throw e;
        }
    }

    private static Mono<Void> withGatewayClient(GatewayDiscordClient gateway) {
        final RestClient restClient = gateway.getRestClient();
        final ApplicationService applicationService = restClient.getApplicationService();
        final long applicationId = restClient.getApplicationId().block();
        final List<ApplicationCommandRequest> request = new ArrayList<>();
        request.add(ApplicationCommandRequest.builder()
            .name("fail").description("causes bot to freeze").dmPermission(false).build());

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, request)
            .doOnNext(cmd -> LOGGER.info("registered /fail"))
            .doOnError(e -> {
                    LOGGER.error("could not register /fail", e);
                    System.exit(1);
                })
            .then()
            .block();

        return Mono.when(
            gateway.on(ChatInputInteractionEvent.class, event -> handleChatCommand(event)),
            gateway.on(ButtonInteractionEvent.class, event -> handleButtonInteraction(event)));
    }
}
