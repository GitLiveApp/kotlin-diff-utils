package dev.gitlive.difflib.merge

import org.junit.Assert.*
import org.junit.Test


class MergeTest {
    @Test
    fun threeWayDongTest() {
        val base =  "touch\nmy\ndong"
        val left =  "touch\nmy\ndong"
        val right = "touch\nmy\ndong"
        val merged = merge(left, base, right)
        assertFalse(merged.conflict)
        assertEquals((merged.joinedResults() as List<*>).first(), "touch\nmy\ndong")
    }

    @Test
    fun simpleMergeTest() {
        val merged = merge("foo", "foo", "bar")
        assertFalse(merged.conflict)
        assertEquals(merged.joinedResults(), listOf("bar"))
    }

    @Test
    fun mergeConflictTest() {
        val merged = merge("foo", "bar", "baz")
        assertTrue(merged.conflict)
    }

    @Test
    fun twoDifferentMergesTest() {
        val base =      listOf(1,2,3,4,5,6).joinToString("\n")
        val left =      listOf(2,3,4,5,6).joinToString("\n")
        val right=      listOf(1,2,3,4,9,6).joinToString("\n")
        val expected=   listOf(2,3,4,9,6).joinToString("\n")

        val merged = merge(left, base, right)
        assertFalse(merged.conflict)
        assertEquals((merged.joinedResults() as List<*>).first(), expected)
    }

    @Test
    fun coreMergeFailTest() {
        val left = "package dev.bluefalcon\n" +
                "\n" +
                "import android.bluetooth.BluetoothGattService\n" +
                "\n" +
                "actual class BluetoothService(val service: BluetoothGattService) {\n" +
                "    actual val name: String?\n" +
                "        get() = service.uuid.toString()\n" +
                "    actual val chars: List<BluetoothCharacteristic>\n" +
                "        get() = service.characteristics.map {\n" +
                "            BluetoothCharacteristic(it)\n" +
                "        }\n" +
                "    val anotherChange = \"Test\"\n" +
                "    val commitAhead = \"Ahead.PartTwo\"\n" +
                "    val moveMaster = \"furtherAhead\"\n" +
                "}"
        val base = "package dev.bluefalcon\n" +
                "\n" +
                "import android.bluetooth.BluetoothGattService\n" +
                "\n" +
                "actual class BluetoothService(val service: BluetoothGattService) {\n" +
                "    actual val name: String?\n" +
                "        get() = service.uuid.toString()\n" +
                "    actual val chars: List<BluetoothCharacteristic>\n" +
                "        get() = service.characteristics.map {\n" +
                "            BluetoothCharacteristic(it)\n" +
                "        }\n" +
                "    val anotherChange = \"Test\"\n" +
                "    val commitAhead = \"Ahead.PartTwo\"\n" +
                "}"
        val right = "package dev.bluefalcon\n" +
                "\n" +
                "import android.bluetooth.BluetoothGattService\n" +
                " fsdfdsf sdfdssdf\n" +
                "actual class BluetoothService(val service: BluetoothGattService) {\n" +
                "    dsdad sad sadsad sa\n" +
                "    dasdsa\n" +
                "\n" +
                "    dasdsa\n" +
                "    actual val name: String?\n" +
                "        get() = service.uuid.toString()\n" +
                "    actual val chars: List<BluetoothCharacteristic>\n" +
                "        get() = service.characteristics.map {\n" +
                "            BluetoothCharacteristic(it)\n" +
                "        }\n" +
                "    val anotherChange = \"Test\"\n" +
                "    val commitAhead = \"Ahead.PartTwo\"\n" +
                "    val changeBeforeMaster = \"Hi\"\n" +
                "}"

        val merged = merge(left, base, right)
        assertTrue(merged.conflict)
        assertEquals("package dev.bluefalcon\n" +
                "\n" +
                "import android.bluetooth.BluetoothGattService\n" +
                "<<<<<<< LEFT\n" +
                "\n" +
                "actual class BluetoothService(val service: BluetoothGattService) {\n" +
                "    actual val name: String?\n" +
                "        get() = service.uuid.toString()\n" +
                "    actual val chars: List<BluetoothCharacteristic>\n" +
                "        get() = service.characteristics.map {\n" +
                "            BluetoothCharacteristic(it)\n" +
                "        }\n" +
                "    val anotherChange = \"Test\"\n" +
                "    val commitAhead = \"Ahead.PartTwo\"\n" +
                "    val moveMaster = \"furtherAhead\"\n" +
                "=======\n" +
                " fsdfdsf sdfdssdf\n" +
                "actual class BluetoothService(val service: BluetoothGattService) {\n" +
                "    dsdad sad sadsad sa\n" +
                "    dasdsa\n" +
                "\n" +
                "    dasdsa\n" +
                "    actual val name: String?\n" +
                "        get() = service.uuid.toString()\n" +
                "    actual val chars: List<BluetoothCharacteristic>\n" +
                "        get() = service.characteristics.map {\n" +
                "            BluetoothCharacteristic(it)\n" +
                "        }\n" +
                "    val anotherChange = \"Test\"\n" +
                "    val commitAhead = \"Ahead.PartTwo\"\n" +
                "    val changeBeforeMaster = \"Hi\"\n" +
                ">>>>>>> RIGHT\n" +
                "}", merged.joinedResults())
    }
}