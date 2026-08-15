package io.github.spearchucker667.veniceforge.core.data

import androidx.room.TypeConverter
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationKind
import io.github.spearchucker667.veniceforge.core.data.entity.MessageRole
import io.github.spearchucker667.veniceforge.core.data.entity.MessageStatus
import io.github.spearchucker667.veniceforge.core.data.entity.ToolCallStatus

class Converters {
    @TypeConverter fun fromMessageRole(v: MessageRole?): String? = v?.name
    @TypeConverter fun toMessageRole(v: String?): MessageRole? = v?.let { MessageRole.valueOf(it) }

    @TypeConverter fun fromMessageStatus(v: MessageStatus?): String? = v?.name
    @TypeConverter fun toMessageStatus(v: String?): MessageStatus? = v?.let { MessageStatus.valueOf(it) }

    @TypeConverter fun fromConversationKind(v: ConversationKind?): String? = v?.name
    @TypeConverter fun toConversationKind(v: String?): ConversationKind? = v?.let { ConversationKind.valueOf(it) }

    @TypeConverter fun fromToolCallStatus(v: ToolCallStatus?): String? = v?.name
    @TypeConverter fun toToolCallStatus(v: String?): ToolCallStatus? = v?.let { ToolCallStatus.valueOf(it) }
}
