"use client";

import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import {
    useForm,
    type Resolver,
    type UseFormReturn,
    type SubmitHandler,
} from "react-hook-form";
import { motion } from "framer-motion";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

const schema = z.object({
    name: z.string().min(1, "이름은 필수입니다."),
    symbol: z.string().min(1, "심볼은 필수입니다."),
    network: z.string().min(1, "네트워크는 필수입니다."),
    decimals: z.coerce.number().int().min(0, "0 이상의 정수"),
    iconUrl: z.string().url("정확한 URL을 입력하세요.").optional().or(z.literal("")),
});
type FormValues = z.infer<typeof schema>;

const fadeInUp = {
    hidden: { opacity: 0, y: 16 },
    show: { opacity: 1, y: 0, transition: { duration: 0.4 } },
};

const stagger = (delay = 0.1) => ({
    hidden: {},
    show: {
        transition: {
            staggerChildren: delay,
        },
    },
});

export default function AdminCoinNewPage() {
    const resolver = zodResolver(schema) as Resolver<FormValues>;
    const form = useForm<FormValues>({
        resolver,
        defaultValues: { name: "", symbol: "", network: "", decimals: 8, iconUrl: "" },
    });

    const onSubmit: SubmitHandler<FormValues> = async (values) => {
        const res = await fetch("/api/coins", {
            method: "POST",
            body: JSON.stringify(values),
        });
        console.log(await res.json());
    };

    return (
        <motion.div
            className="container py-8 max-w-lg"
            variants={stagger(0.08)}
            initial="hidden"
            animate="show"
        >
            <motion.h2
                variants={fadeInUp}
                className="text-xl font-semibold mb-4"
            >
                가상화폐 추가 (Admin)
            </motion.h2>
            <motion.form
                variants={stagger(0.05)}
                onSubmit={form.handleSubmit(onSubmit)}
                className="space-y-4"
            >
                <motion.div variants={fadeInUp}>
                    <Field name="name" label="이름" placeholder="Bitcoin" form={form} />
                </motion.div>
                <motion.div variants={fadeInUp}>
                    <Field name="symbol" label="심볼" placeholder="BTC" form={form} />
                </motion.div>
                <motion.div variants={fadeInUp}>
                    <Field name="network" label="네트워크" placeholder="Bitcoin / ERC-20 ..." form={form} />
                </motion.div>
                <motion.div variants={fadeInUp}>
                    <Field name="decimals" label="소수점 자리(decimals)" type="number" form={form} registerOptions={{ valueAsNumber: true }} />
                </motion.div>
                <motion.div variants={fadeInUp}>
                    <Field name="iconUrl" label="아이콘 URL" placeholder="https://..." form={form} />
                </motion.div>
                <motion.div variants={fadeInUp}>
                    <Button type="submit" className="w-full transition hover:scale-[1.02] active:scale-[0.99]">추가</Button>
                </motion.div>
            </motion.form>
        </motion.div>
    );
}

type FieldProps = {
    name: keyof FormValues;
    label: string;
    placeholder?: string;
    type?: React.InputHTMLAttributes<HTMLInputElement>["type"];
    registerOptions?: Parameters<UseFormReturn<FormValues>["register"]>[1];
    form: UseFormReturn<FormValues>;
};

function Field({ name, label, placeholder, type = "text", registerOptions, form }: FieldProps) {
    const { register, formState: { errors } } = form;
    return (
        <div className="space-y-2">
            <Label htmlFor={name}>{label}</Label>
            <Input id={name} type={type} placeholder={placeholder} {...register(name, registerOptions)} />
            {errors[name]?.message && (
                <p className="text-sm text-red-500">{String(errors[name]?.message)}</p>
            )}
        </div>
    );
}
