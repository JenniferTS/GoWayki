        package pe.edu.tecsup.gowayki.domain.lines

        fun shareLineText(line: LineSummary): String {
            val agency = line.agency.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            return "Línea ${line.name}$agency · gowayki go
https://gowayki.app/line/${line.id}"
        }
