from pydantic import BaseModel

class OriginalText(BaseModel):
    originalText:str

class QuizRequest(BaseModel):
    summaryText:str
    
