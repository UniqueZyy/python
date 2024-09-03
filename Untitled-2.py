
import os
import uuid
from typing import List
from fastapi import FastAPI, File, UploadFile
from PIL import Image

app = FastAPI()

@app.post("/upload/")
async def upload(file: UploadFile = File(...)):
    file_name = file.filename
    file_type = file_name.split(".")[-1]
    content_type = file.content_type
    type = content_type.split("/")[-1]
    
    result = (type == "pdf" and file_type == "pdf") or (type in ["jpeg", "jpg"] and file_type in ["jpeg", "jpg"]) or(type == "png" and file_type == "png")
    
    if not result:
        return {"message": "文件格式不符合"}
    
    new_file_name = f"{uuid.uuid4()}.{file_type}"
    output_path = os.path.join("static", new_file_name)
    
    if not os.path.exists("static"):
        os.makedirs("static")
    
    with open(output_path, "wb") as buffer:
        buffer.write(await file.read())
    
    if type == "pdf":
        # 调用PdfToPng方法
        # output_path = PdfToPng(file)
        pass
    
    return {"output_path": output_path}
