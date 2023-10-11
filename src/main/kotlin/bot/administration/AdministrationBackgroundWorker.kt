package codes.rorak.hamley.bot.administration

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.administration.Administration.unbanMember
import codes.rorak.hamley.bot.administration.Administration.unmuteMember
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import codes.rorak.hamley.util.toMemberOrNull
import codes.rorak.hamley.util.toUser
import java.time.OffsetDateTime
import java.util.*

object AdministrationBackgroundWorker: Runnable {
    override fun run() {
        Timer("ADM-Unpunisher").scheduleAtFixedRate(unpunisher, 0, 60000);
    }

    private val unpunisher = object : TimerTask() {
        override fun run() {
            // check mute
            db.mutelist.forEach { (user, value) ->
                if (value.unmute == null) return@forEach;
                if (OffsetDateTime.now().isBefore(value.unmute)) return@forEach;
                try {
                    unmuteMember(
                        user.toMemberOrNull(),
                        bot.guild.selfMember,
                        messages.constants.timeRanOut
                    );
                }
                catch (ex: Throwable) {
                    db.mutelist.remove(user);
                    saveDb();
                }
            };
            // check ban
            db.banlist.forEach { (user, value) ->
                if (value.unban == null) return@forEach;
                if (OffsetDateTime.now().isBefore(value.unban)) return@forEach;
                unbanMember(
                    user.toUser().name,
                    bot.guild.selfMember,
                    messages.constants.timeRanOut
                );
            };
        }
    };
}