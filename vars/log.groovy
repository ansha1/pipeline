def END_CHAR = '\033[0m '
def YELLOW_BOLD = '\033[1;33m'
def BLUE_BOLD = '\033[1;34m'
def RED_BOLD = '\033[1;31m'


def printColor(String message, String color){
    ansiColor('xterm') {
        echo "${color}${msg}${END_CHAR}"
    }
}

def info(message) {
    printColor(message, BLUE_BOLD)
}

def warning(message) {
    printColor(message, YELLOW_BOLD)
}

def error(message) {
    printColor(message, RED_BOLD)
}
