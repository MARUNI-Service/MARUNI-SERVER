from openai import OpenAI
from dotenv import load_dotenv
import os

# .env 파일 로드
load_dotenv()

# 환경 변수에서 API 키 불러오기
api_key = os.getenv("OPENAI_API_KEY")

# OpenAI 클라이언트 생성
client = OpenAI(api_key=api_key)

# 오디오 파일 열기
audio_file = open("노인남여_노인대화07_F_1522434093_60_경상_실내_08589.wav", "rb")

# STT 요청
transcription = client.audio.transcriptions.create(
    model="whisper-1",    # 또는 "gpt-4o-transcribe"
    file=audio_file,
    language="ko"         # 한국어 명시
)

# 결과 출력
print(transcription.text)
