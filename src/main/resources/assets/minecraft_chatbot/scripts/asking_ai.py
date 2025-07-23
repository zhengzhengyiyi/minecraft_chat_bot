import ollama
from argparse import ArgumentParser

def ask_ollama(prompt):
    response = ollama.chat(
        # model="phi3",
        model="tinyllama",
        format="json",
        messages=[
            {"role": "system", "content": f"You are an AI assisstant in Minecraft {version} which needs to help player, your name is ollama, do not care that much if player call your name wrong. Be nice to the player, do not make the player get angrey. If the player is aking you about a detail about a Minecraft feature or game type, please answer it detaily. Try to get the point of the question, do not think too much, and using simple language. Do not chat about other thing (not Minecraft) that much."},
            {"role": "user", "content": prompt}
        ],
    )
    # return response.model_dump_json()
    return response["message"]["content"]

if __name__ == '__main__':
    parser = ArgumentParser("Run pretrained models on MineRL environment")

    parser.add_argument("--version", type=str, required=True, help="pass into the Minecraft version")
    parser.add_argument("--prompt", type=str, required=True, help="the thing that need to ask to ai")

    args = parser.parse_args()

    version = args.version
    prompt = args.prompt

    print(ask_ollama(prompt), flush=True)
