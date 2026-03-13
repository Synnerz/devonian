package com.github.synnerz.devonian.features.misc.tooltip

import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.events.TooltipRenderEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.FixedIdentityMap
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ItemAge : Feature(
    "itemAge",
    "shows age of item/timestamp when created",
    subcategory = "Tooltip",
) {
    private val SETTING_SHOW_TIMESTAMP = addSelection(
        "timestamp",
        2,
        listOf("Never", "Always", "Holding Shift"),
        "",
        "Show Item Timestamp",
    )
    private val SETTING_SHOW_ITEM_AGE = addSelection(
        "age",
        1,
        listOf("Never", "Always", "Holding Shift"),
        "",
        "Show Item Age",
    )

    private val timestamps = FixedIdentityMap<ItemStack, ZonedDateTime>(128)
    private val EMPTY = ZonedDateTime.now()

    override fun initialize() {
        on<TooltipRenderEvent> { event ->
            val item = event.item ?: return@on

            val time = timestamps.getOrPut(item) {
                // https://github.com/cow-mc/Cowlection/blob/6a27732242abd310cb0d6fc6f5268382dea45b16/src/main/java/de/cowtipper/cowlection/listener/skyblock/SkyBlockListener.java#L275
                val data = ItemUtils.extraAttributes(item) ?: return@getOrPut EMPTY

                val timestamp = data.get("timestamp") ?: return@getOrPut EMPTY

                val timeLong = timestamp.asLong().getOrNull()
                if (timeLong != null) {
                    return@getOrPut ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(timeLong),
                        ZoneId.systemDefault(),
                    )
                }

                val timeStr = timestamp.asString().getOrNull() ?: return@getOrPut EMPTY

                val ldt = try {
                    if (timeStr.endsWith("M")) {
                        // format: month > day > year + 12-hour clock (AM or PM)
                        LocalDateTime.parse(
                            timeStr,
                            DateTimeFormatter.ofPattern("M/d/yy h:mm a", Locale.US)
                        )
                    } else {
                        // format: day > month > year + 24-hour clock (very, very rare)
                        LocalDateTime.parse(
                            timeStr,
                            DateTimeFormatter.ofPattern("d/M/yy HH:mm", Locale.US)
                        )
                    }
                } catch(_: DateTimeParseException) {
                    return@getOrPut EMPTY
                }

                ZonedDateTime.of(ldt, ZoneId.of("America/Toronto"))
                    .withZoneSameInstant(ZoneId.systemDefault())
            }

            // Identity-sensitive operation on an instance of value type 'ZonedDateTime!' may cause unexpected behavior or errors
            if (time === EMPTY) return@on

            if (
                when (SETTING_SHOW_TIMESTAMP.get()) {
                    1 -> true
                    2 -> event.shift
                    else -> false
                }
            ) {
                val formatted = FormattedCharSequence.composite(
                    FormattedCharSequence.forward("Timestamp: ", Style.EMPTY.withColor(ChatFormatting.GRAY)),
                    FormattedCharSequence.forward(
                        time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm zzz")),
                        Style.EMPTY.withColor(ChatFormatting.DARK_GRAY),
                    ),
                )

                event.lore.add(ClientTooltipComponent.create(formatted))
            }

            if (
                when (SETTING_SHOW_ITEM_AGE.get()) {
                    1 -> true
                    2 -> event.shift
                    else -> false
                }
            ) {
                val creationTime = time.toEpochSecond() * 1000L
                val itemAge = System.currentTimeMillis() - creationTime

                val formatted = FormattedCharSequence.composite(
                    FormattedCharSequence.forward("Item age: ", Style.EMPTY.withColor(ChatFormatting.GRAY)),
                    FormattedCharSequence.forward(
                        if (itemAge >= 60_000L) StringUtils.formatDuration(itemAge)
                        else "<1 minute",
                        Style.EMPTY.withColor(ChatFormatting.DARK_GRAY),
                    ),
                )

                event.lore.add(ClientTooltipComponent.create(formatted))
            }
        }.prio = 1
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        timestamps.clear()
    }
}