package io.github.spearchucker667.veniceforge.core.data

import androidx.room.TypeConverter
import io.github.spearchucker667.veniceforge.core.data.entity.ConversationKind
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaKind
import io.github.spearchucker667.veniceforge.core.data.entity.GeneratedMediaOperation
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

    @TypeConverter fun fromGeneratedMediaKind(v: GeneratedMediaKind?): String? = v?.name
    @TypeConverter fun toGeneratedMediaKind(v: String?): GeneratedMediaKind? = v?.let { GeneratedMediaKind.valueOf(it) }

    @TypeConverter fun fromGeneratedMediaOperation(v: GeneratedMediaOperation?): String? = v?.name
    @TypeConverter fun toGeneratedMediaOperation(v: String?): GeneratedMediaOperation? = v?.let { GeneratedMediaOperation.valueOf(it) }
}
