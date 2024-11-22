import os


def get_parameter(name: str) -> str:
    file_name = os.getenv(f"{name}_FILE")

    if file_name is not None:
        with open(file_name, 'rt') as f:
            return f.readline()

    env_value = os.getenv(name)

    if env_value is None:
        raise Exception(f"There is no env parameter defined as \"{name}\" or \"{file_name}\".")

    return env_value
