# OpenAI plug-in for OmegaT

This plug-in enables users to utilize OpenAI API for machine translation within the OmegaT CAT environment.

![](images/demo.png)

Originally based on the [Naver Papago plug-in for OmegaT](https://github.com/ParanScreen/omegat-plugin-navertranslate), this plug-in has evolved significantly.

This software is open source software licensed under the GNU GPLv2. In addition, as a special exception, the copyright holders of this program give you permission to combine the program with free software programs or libraries that are released with code included in the standard release of [JSON-java Library](https://github.com/stleary/JSON-java) under the [JSON-java License](https://github.com/stleary/JSON-java/blob/master/LICENSE). You may copy and distribute such a system following the terms of the GNU GPL for this program and the licenses of the other code concerned. For detailed information, please refer to the LICENSE file.

## Key Features

- **Machine Translation with OpenAI**: Use the OpenAI API to translate text within OmegaT.
- **Glossary Integration**: Automatically incorporates glossary terms into translations to improve accuracy.
- **Tag Preservation**: Ensures that tags are preserved during the translation process, maintaining the structure and formatting of the original text.
- **Built-in Caching**: Leverages OmegaT's built-in caching mechanism to improve translation request handling and efficiency.
- **Configurable Model and Temperature**: Allows users to configure the OpenAI model and temperature settings via system properties.

## How to use

1. Get your API key from [OpenAI](https://platform.openai.com/account/api-keys).

2. Copy the plug-in file into directory:

    - Windows: Copy the plug-in file into %SystemDrive%%ProgramFiles%\OmegaT\plugins directory.
    - macOS: Copy the plug-in file into /Applications/OmegaT.app/Contents/Java/plugins directory.
    - GNU/Linux: Copy the plug-in file under the directory that OmegaT installed.

3. Set your OpenAI key.

    - Windows: Open the file %SystemDrive%\%ProgramFiles%\OmegaT\OmegaT.I4J.ini and add this line:
    ```
    -Dopenai.api.key=YOURAPIKEY
    ```

    - macOS: Open the file /Applications/OmegaT.app/Contents/Resources/Configuration.properties with text editor and add this line:
    ```
    openai.api.key=YOURAPIKEY
    ```
   
    - In case of running the program with command line prompt, add this parameter:
    ```
    -Dopenai.api.key=YOURAPIKEY
    ```
   
4. (Optional) Configure model and temperature:

   - Model (default is `gpt-4o`):

     ```
     -Dopenai.model=YOURMODELNAME
     ```

     Example: `-Dopenai.model=gpt-4o-mini`

   - Temperature (default is `0`):

     ```
     -Dopenai.temperature=YOURTEMPERATURE
     ```

     Example: `-Dopenai.temperature=0.5`

   Add these to the same configuration files as in step 3, or use as command line parameters.

5. Open OmegaT Application. From Options > Machine Translation, select OpenAI Translate.

6. (Optional) You would want to uncheck "Enable Sentence-level Segmenting" to get more fluent translation.

    ![](images/disable_sentence-level_segmenting.png)
