package com.github.rwsbillyang.wxSDK.work.pay




/**
 * 参照KotlinxSerializer实现
 * */
//@OptIn(
//    ImplicitReflectionSerializer::class, UnstableDefault::class
//)
//class XmlSerializer( private val xml: XML =  XML { autoPolymorphic = true }) : JsonSerializer {
//    override fun read(type: TypeInfo, body: Input): Any {
//        val text = body.readText()
//        val mapper = type.kotlinType?.let { serializer(it) } ?: type.type.serializer()
//        return xml.parse(mapper, text)!!
//    }
//
//    override fun write(data: Any, contentType: ContentType): OutgoingContent {
//        @Suppress("UNCHECKED_CAST")
//
//        val content = xml.stringify(buildSerializer(data) as KSerializer<Any>, data)
//        return TextContent(content, contentType)
//    }
//
//}
//
//
//@Suppress("UNCHECKED_CAST")
//@OptIn(ImplicitReflectionSerializer::class)
//private fun buildSerializer(value: Any): KSerializer<*> = when (value) {
//    is Element -> ElementSerializer
//    is List<*> -> value.elementSerializer().list
//    is Array<*> -> value.firstOrNull()?.let { buildSerializer(it) } ?: String.serializer().list
//    is Set<*> -> value.elementSerializer().set
//    is Map<*, *> -> {
//        val keySerializer = value.keys.elementSerializer() as KSerializer<Any>
//        val valueSerializer = value.values.elementSerializer() as KSerializer<Any>
//        MapSerializer(keySerializer, valueSerializer)
//    }
//    else -> value::class.serializer()
//}
//
//@OptIn(ImplicitReflectionSerializer::class)
//private fun Collection<*>.elementSerializer(): KSerializer<*> {
//    @Suppress("DEPRECATION_ERROR")
//    val serializers = filterNotNull().map { buildSerializer(it) }.distinctBy { it.descriptor.name }
//
//    if (serializers.size > 1) {
//        @Suppress("DEPRECATION_ERROR")
//        error(
//            "Serializing collections of different element types is not yet supported. " +
//                    "Selected serializers: ${serializers.map { it.descriptor.name }}"
//        )
//    }
//
//    val selected = serializers.singleOrNull() ?: String.serializer()
//
//    if (selected.descriptor.isNullable) {
//        return selected
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    selected as KSerializer<Any>
//
//    if (any { it == null }) {
//        return selected.nullable
//    }
//
//    return selected
//}

//val xmlClient = HttpClient(CIO) {
//    install(JsonFeature) {
//        serializer = XmlSerializer()
//        acceptContentTypes = listOf(ContentType.Application.Xml)
//    }
//}

